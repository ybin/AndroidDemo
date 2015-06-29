/* binder.c
 *
 * Android IPC Subsystem
 *
 * Copyright (C) 2007-2008 Google, Inc.
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

#define pr_fmt(fmt) KBUILD_MODNAME ": " fmt

#include <asm/cacheflush.h>
#include <linux/fdtable.h>
#include <linux/file.h>
#include <linux/fs.h>
#include <linux/list.h>
#include <linux/miscdevice.h>
#include <linux/mm.h>
#include <linux/module.h>
#include <linux/mutex.h>
#include <linux/nsproxy.h>
#include <linux/poll.h>
#include <linux/debugfs.h>
#include <linux/rbtree.h>
#include <linux/sched.h>
#include <linux/seq_file.h>
#include <linux/uaccess.h>
#include <linux/vmalloc.h>
#include <linux/slab.h>
#include <linux/pid_namespace.h>

#include "binder.h"
#include "binder_trace.h"

static DEFINE_MUTEX(binder_main_lock);
static DEFINE_MUTEX(binder_deferred_lock);
static DEFINE_MUTEX(binder_mmap_lock);

static HLIST_HEAD(binder_procs);
static HLIST_HEAD(binder_deferred_list);
static HLIST_HEAD(binder_dead_nodes);

static struct dentry *binder_debugfs_dir_entry_root;
static struct dentry *binder_debugfs_dir_entry_proc;
static struct binder_node *binder_context_mgr_node;
static kuid_t binder_context_mgr_uid = INVALID_UID;
static int binder_last_id;
static struct workqueue_struct *binder_deferred_workqueue;

#define BINDER_DEBUG_ENTRY(name) \
static int binder_##name##_open(struct inode *inode, struct file *file) \
{ \
	return single_open(file, binder_##name##_show, inode->i_private); \
} \
\
static const struct file_operations binder_##name##_fops = { \
	.owner = THIS_MODULE, \
	.open = binder_##name##_open, \
	.read = seq_read, \
	.llseek = seq_lseek, \
	.release = single_release, \
}

static int binder_proc_show(struct seq_file *m, void *unused);
BINDER_DEBUG_ENTRY(proc);

/* This is only defined in include/asm-arm/sizes.h */
#ifndef SZ_1K
#define SZ_1K                               0x400
#endif

#ifndef SZ_4M
#define SZ_4M                               0x400000
#endif

#define FORBIDDEN_MMAP_FLAGS                (VM_WRITE)

#define BINDER_SMALL_BUF_SIZE (PAGE_SIZE * 64)

enum {
	BINDER_DEBUG_USER_ERROR             = 1U << 0,
	BINDER_DEBUG_FAILED_TRANSACTION     = 1U << 1,
	BINDER_DEBUG_DEAD_TRANSACTION       = 1U << 2,
	BINDER_DEBUG_OPEN_CLOSE             = 1U << 3,
	BINDER_DEBUG_DEAD_BINDER            = 1U << 4,
	BINDER_DEBUG_DEATH_NOTIFICATION     = 1U << 5,
	BINDER_DEBUG_READ_WRITE             = 1U << 6,
	BINDER_DEBUG_USER_REFS              = 1U << 7,
	BINDER_DEBUG_THREADS                = 1U << 8,
	BINDER_DEBUG_TRANSACTION            = 1U << 9,
	BINDER_DEBUG_TRANSACTION_COMPLETE   = 1U << 10,
	BINDER_DEBUG_FREE_BUFFER            = 1U << 11,
	BINDER_DEBUG_INTERNAL_REFS          = 1U << 12,
	BINDER_DEBUG_BUFFER_ALLOC           = 1U << 13,
	BINDER_DEBUG_PRIORITY_CAP           = 1U << 14,
	BINDER_DEBUG_BUFFER_ALLOC_ASYNC     = 1U << 15,
};
static uint32_t binder_debug_mask = BINDER_DEBUG_USER_ERROR |
	BINDER_DEBUG_FAILED_TRANSACTION | BINDER_DEBUG_DEAD_TRANSACTION;
module_param_named(debug_mask, binder_debug_mask, uint, S_IWUSR | S_IRUGO);

static bool binder_debug_no_lock;
module_param_named(proc_no_lock, binder_debug_no_lock, bool, S_IWUSR | S_IRUGO);

static DECLARE_WAIT_QUEUE_HEAD(binder_user_error_wait);
static int binder_stop_on_user_error;

static int binder_set_stop_on_user_error(const char *val,
					 struct kernel_param *kp)
{
	int ret;
	ret = param_set_int(val, kp);
	if (binder_stop_on_user_error < 2)
		wake_up(&binder_user_error_wait);
	return ret;
}
module_param_call(stop_on_user_error, binder_set_stop_on_user_error,
	param_get_int, &binder_stop_on_user_error, S_IWUSR | S_IRUGO);

#define binder_debug(mask, x...) \
	do { \
		if (binder_debug_mask & mask) \
			pr_info(x); \
	} while (0)

#define binder_user_error(x...) \
	do { \
		if (binder_debug_mask & BINDER_DEBUG_USER_ERROR) \
			pr_info(x); \
		if (binder_stop_on_user_error) \
			binder_stop_on_user_error = 2; \
	} while (0)

enum binder_stat_types {
	BINDER_STAT_PROC,
	BINDER_STAT_THREAD,
	BINDER_STAT_NODE,
	BINDER_STAT_REF,
	BINDER_STAT_DEATH,
	BINDER_STAT_TRANSACTION,
	BINDER_STAT_TRANSACTION_COMPLETE,
	BINDER_STAT_COUNT
};

struct binder_stats {
	int br[_IOC_NR(BR_FAILED_REPLY) + 1];
	int bc[_IOC_NR(BC_DEAD_BINDER_DONE) + 1];
	int obj_created[BINDER_STAT_COUNT];
	int obj_deleted[BINDER_STAT_COUNT];
};

static struct binder_stats binder_stats;

static inline void binder_stats_deleted(enum binder_stat_types type)
{
	binder_stats.obj_deleted[type]++;
}

static inline void binder_stats_created(enum binder_stat_types type)
{
	binder_stats.obj_created[type]++;
}

struct binder_transaction_log_entry {
	int debug_id;
	int call_type;
	int from_proc;
	int from_thread;
	int target_handle;
	int to_proc;
	int to_thread;
	int to_node;
	int data_size;
	int offsets_size;
};
struct binder_transaction_log {
	int next;
	int full;
	struct binder_transaction_log_entry entry[32];
};
static struct binder_transaction_log binder_transaction_log;
static struct binder_transaction_log binder_transaction_log_failed;

static struct binder_transaction_log_entry *binder_transaction_log_add(
	struct binder_transaction_log *log)
{
	struct binder_transaction_log_entry *e;
	e = &log->entry[log->next];
	memset(e, 0, sizeof(*e));
	log->next++;
	if (log->next == ARRAY_SIZE(log->entry)) {
		log->next = 0;
		log->full = 1;
	}
	return e;
}









// work用于proc->todo或者thread->todo，即构成进程或者线程的todo list，
// binder_work一般作为更大的结构体(如binder_thread, binder_node)的一个字段
struct binder_work {
	struct list_head entry;
	enum {
		BINDER_WORK_TRANSACTION = 1, // A->B，则B的todo list上会挂载一个transaction type的work
		BINDER_WORK_TRANSACTION_COMPLETE, // A->B，则A往driver发送指令，driver将transaction转换完毕，并将相关work交给B之后，会给A的todo list上挂载一个该类型的work
		BINDER_WORK_NODE, // node相关的操作，即引用计数增减
		BINDER_WORK_DEAD_BINDER,
		BINDER_WORK_DEAD_BINDER_AND_CLEAR,
		BINDER_WORK_CLEAR_DEATH_NOTIFICATION,
	} type;
};

struct binder_node {
	int debug_id;
	struct binder_work work; // node可以是一种work，如强弱指针修改引用计数的时候
	union {
		struct rb_node rb_node; // node要放到其所在进程的node集合中，这个就是索引点，见binder_proc->nodes
		struct hlist_node dead_node; // node也可能是dead的，此时将其加入binder_dead_nodes(全局变量)列表
	};
	struct binder_proc *proc; // 所属的proc
	struct hlist_head refs; // 该node的弱引用列表，即都有谁弱引用我了
	
	int internal_strong_refs;
	int local_weak_refs;
	int local_strong_refs;
	unsigned has_strong_ref:1;
	unsigned pending_strong_ref:1;
	unsigned has_weak_ref:1;
	unsigned pending_weak_ref:1;
	unsigned has_async_transaction:1;
	unsigned accept_fds:1;
	unsigned min_priority:8;
	
	struct list_head async_todo;

	void __user *ptr;	// user space对象的弱引用对象(RefBase::weakref_type*)
	void __user *cookie; // user space里的实际对象指针
};

struct binder_ref_death {
	struct binder_work work;
	void __user *cookie;
};

struct binder_ref {
	/* Lookups needed: */
	/*   node + proc => ref (transaction) */
	/*   desc + proc => ref (transaction, inc/dec ref) */
	/*   node => refs + procs (proc exit) */
	int debug_id;
	struct rb_node rb_node_desc; // 挂载点，对应到binder_proc->refs_by_desc
	struct rb_node rb_node_node; // 挂载点，对应到binder_proc->refs_by_node
	struct hlist_node node_entry;
	
	struct binder_proc *proc; // ref所关联的proc
	struct binder_node *node; // ref所关联的node
	struct binder_ref_death *death; // ref所关联的death
	
	uint32_t desc; /* rb_node_desc排序使用该值，rb tree判断左子树还是又子树的依据
				       rb_node_node排序是的是binder_node的内存地址，所以无需额外的值
				       desc，其实就是user space里的handle.
				    */
	int strong;
	int weak;
};

struct binder_buffer {
	struct list_head entry; /* free and allocated entries by address */
	struct rb_node rb_node; /* free entry by size or allocated entry */
							  /* by address */
	unsigned free:1;
	unsigned allow_user_free:1; // 是否允许user space释放该buffer，因为mmap的关系，client buffer跟kernel buffer是同一块物理内存
								  // 发送端送出的buffer不允许释放，要等到接收端接收并处理完数据之后才能运行用户释放
	unsigned async_transaction:1;
	unsigned debug_id:29;

	struct binder_transaction *transaction; // buffer所关联的transaction
	struct binder_node *target_node;	// buffer所关联的node
	
	size_t data_size;
	size_t offsets_size;
	uint8_t data[0];
};

enum binder_deferred_state {
	BINDER_DEFERRED_PUT_FILES    = 0x01,
	BINDER_DEFERRED_FLUSH        = 0x02,
	BINDER_DEFERRED_RELEASE      = 0x04,
};

// 每个使用binder的进程在kernel中拥有一个struct binder_proc结构
struct binder_proc {
	struct hlist_node proc_node; // 把每个binder_proc加入列表(binder_procs，全局变量)中
	struct rb_root threads; // 每个进程包含的thread集合
	struct rb_root nodes;   // 每个进程有一个node集合
	struct rb_root refs_by_desc; // 每个进程包含的refs，按照desc(binder_ref->desc)进行索引
	struct rb_root refs_by_node; // 每个进程包含的refs，按照node(node的内存地址)进行索引
	int pid; // 进程id
	struct vm_area_struct *vma;
	struct mm_struct *vma_vm_mm;
	struct task_struct *tsk; // 进程pcb
	struct files_struct *files; // 进程打开的文件
	struct hlist_node deferred_work_node;
	int deferred_work;
	void *buffer; // 进程拥有的内核虚拟内存
	ptrdiff_t user_buffer_offset; // buffer在user space和kernel space之间的偏移量

	// 管理buffer的相关数据结构
	struct list_head buffers; // 所有buffer组成的集合
	struct rb_root free_buffers; // 所有free buffer组成的集合
	struct rb_root allocated_buffers; // 所有allocated buffer组成的集合
	size_t free_async_space;

	/*
	  关于二级指针:
	  	pages是一个变量，它保存着一个内存地址，即它指向一个变量，该变量保存的还是一个
	  	内存地址，这个内存地址保存的是实实在在的"值"，其实"内存地址"也是值，它们的区别
	  	只存在于我们的逻辑中。

	  	符合上面的要求，则pages就是一个二级指针。但是实际中，我们会在中间那个变量上做
	  	手脚，我们会创建一列这样的变量(即一个数组)，二级指针不关心中间变量之前、之后的
	  	内存地址上放着什么，但是我们可以利用指针的加减法操作来很方便的定位这一列中间
	  	变量。
	 */
	struct page **pages;
	size_t buffer_size; // 总的buffer大小
	uint32_t buffer_free; // 空闲的buffer大小
	
	struct list_head todo; // binder_work组成的todo list，没有pool thread时，work被放置到proc->todo上
	wait_queue_head_t wait; // 等待队列，todo list为空时，挂起在该等待队列上
	
	struct binder_stats stats; // 用于debug信息统计
	struct list_head delivered_death; // 所有需要通知到的death noti，即进程挂掉之后，该list上的所有death noti都会收到通知
	
	int max_threads; // 进程最多可用的线程，即线程池的最大容量
	int requested_threads; // 线程不够用了，driver request server增大线程池，需要增加这么多个线程
	int requested_threads_started; // server应driver要求，增开了这么多个线程
	int ready_threads; // 当前可以使用的线程数，即处于等待状态的线程数
	long default_priority;
	struct dentry *debugfs_entry;
};

// 线程的状态
enum {
	BINDER_LOOPER_STATE_REGISTERED  = 0x01,
	BINDER_LOOPER_STATE_ENTERED     = 0x02,
	BINDER_LOOPER_STATE_EXITED      = 0x04,
	BINDER_LOOPER_STATE_INVALID     = 0x08,
	BINDER_LOOPER_STATE_WAITING     = 0x10,
	BINDER_LOOPER_STATE_NEED_RETURN = 0x20
};

struct binder_thread {
	struct binder_proc *proc; // thread所属的进程
	struct rb_node rb_node; // 进程的所有thread都要被proc索引到，这个就是索引点，见binder_proc->threads
	int pid; // 即tid
	int looper; // 线程的状态，见上面的enum定义
	struct binder_transaction *transaction_stack; // transaction栈
	struct list_head todo; // struct binder_work组成的列表，这些work有好多种type，见binder_work的定义
	wait_queue_head_t wait; // 等待队列，todo list为空时，挂起在该等待队列上
	
	uint32_t return_error; /* Write failed, return error code in read buf */
	uint32_t return_error2; /* Write failed, return error code in read */
		/* buffer. Used when sending a reply to a dead process that */
		/* we are also waiting on */
	
	struct binder_stats stats; // debug信息统计
};

/*
关于from_parent和to_parent

假设某个transaction T来自线程A，即T->from == A，去往线程B，即T->to_thread == B，
因为每个线程都有一个自己的transaction stack，所以
from_parent指的就是A的transaction stack栈顶，
to_parent指的就是B的transaction stack栈顶
 */
struct binder_transaction {
	int debug_id;
	struct binder_work work; // transaction是一种work
	struct binder_thread *from; // 它从哪个thread来
	struct binder_transaction *from_parent;
	struct binder_proc *to_proc;
	struct binder_thread *to_thread;
	struct binder_transaction *to_parent;
	unsigned need_reply:1;
	/* unsigned is_dead:1; */	/* not used at the moment */

	struct binder_buffer *buffer; // 该transaction的data
	unsigned int	code; // 用户自定义的、业务相关的code
	unsigned int	flags;
	long	priority;
	long	saved_priority;
	kuid_t	sender_euid;
};

static void
binder_defer_work(struct binder_proc *proc, enum binder_deferred_state defer);

static int task_get_unused_fd_flags(struct binder_proc *proc, int flags)
{
	struct files_struct *files = proc->files;
	unsigned long rlim_cur;
	unsigned long irqs;

	if (files == NULL)
		return -ESRCH;

	if (!lock_task_sighand(proc->tsk, &irqs))
		return -EMFILE;

	rlim_cur = task_rlimit(proc->tsk, RLIMIT_NOFILE);
	unlock_task_sighand(proc->tsk, &irqs);

	return __alloc_fd(files, 0, rlim_cur, flags);
}

/*
 * copied from fd_install
 */
static void task_fd_install(
	struct binder_proc *proc, unsigned int fd, struct file *file)
{
	if (proc->files)
		__fd_install(proc->files, fd, file);
}

/*
 * copied from sys_close
 */
static long task_close_fd(struct binder_proc *proc, unsigned int fd)
{
	int retval;

	if (proc->files == NULL)
		return -ESRCH;

	retval = __close_fd(proc->files, fd);
	/* can't restart close syscall because file table entry was cleared */
	if (unlikely(retval == -ERESTARTSYS ||
		     retval == -ERESTARTNOINTR ||
		     retval == -ERESTARTNOHAND ||
		     retval == -ERESTART_RESTARTBLOCK))
		retval = -EINTR;

	return retval;
}

static inline void binder_lock(const char *tag)
{
	trace_binder_lock(tag);
	mutex_lock(&binder_main_lock);
	trace_binder_locked(tag);
}

static inline void binder_unlock(const char *tag)
{
	trace_binder_unlock(tag);
	mutex_unlock(&binder_main_lock);
}

static void binder_set_nice(long nice)
{
	long min_nice;
	if (can_nice(current, nice)) {
		set_user_nice(current, nice);
		return;
	}
	min_nice = 20 - current->signal->rlim[RLIMIT_NICE].rlim_cur;
	binder_debug(BINDER_DEBUG_PRIORITY_CAP,
		     "%d: nice value %ld not allowed use %ld instead\n",
		      current->pid, nice, min_nice);
	set_user_nice(current, min_nice);
	if (min_nice < 20)
		return;
	binder_user_error("%d RLIMIT_NICE not set\n", current->pid);
}

static size_t binder_buffer_size(struct binder_proc *proc,
				 struct binder_buffer *buffer)
{
	if (list_is_last(&buffer->entry, &proc->buffers))
		return proc->buffer + proc->buffer_size - (void *)buffer->data;
	else
		return (size_t)list_entry(buffer->entry.next,
			struct binder_buffer, entry) - (size_t)buffer->data;
}

// 更新free buffer红黑树
static void binder_insert_free_buffer(struct binder_proc *proc,
				      struct binder_buffer *new_buffer)
{
	struct rb_node **p = &proc->free_buffers.rb_node;
	struct rb_node *parent = NULL;
	struct binder_buffer *buffer;
	size_t buffer_size;
	size_t new_buffer_size;

	BUG_ON(!new_buffer->free);

	new_buffer_size = binder_buffer_size(proc, new_buffer);

	binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
		     "%d: add free buffer, size %zd, at %p\n",
		      proc->pid, new_buffer_size, new_buffer);

	while (*p) {
		parent = *p;
		buffer = rb_entry(parent, struct binder_buffer, rb_node);
		BUG_ON(!buffer->free);

		buffer_size = binder_buffer_size(proc, buffer);

		if (new_buffer_size < buffer_size)
			p = &parent->rb_left;
		else
			p = &parent->rb_right;
	}
	rb_link_node(&new_buffer->rb_node, parent, p);
	rb_insert_color(&new_buffer->rb_node, &proc->free_buffers);
}

static void binder_insert_allocated_buffer(struct binder_proc *proc,
					   struct binder_buffer *new_buffer)
{
	struct rb_node **p = &proc->allocated_buffers.rb_node;
	struct rb_node *parent = NULL;
	struct binder_buffer *buffer;

	BUG_ON(new_buffer->free);

	while (*p) {
		parent = *p;
		buffer = rb_entry(parent, struct binder_buffer, rb_node);
		BUG_ON(buffer->free);

		if (new_buffer < buffer)
			p = &parent->rb_left;
		else if (new_buffer > buffer)
			p = &parent->rb_right;
		else
			BUG();
	}
	rb_link_node(&new_buffer->rb_node, parent, p);
	rb_insert_color(&new_buffer->rb_node, &proc->allocated_buffers);
}

static struct binder_buffer *binder_buffer_lookup(struct binder_proc *proc,
						  void __user *user_ptr)
{
	struct rb_node *n = proc->allocated_buffers.rb_node;
	struct binder_buffer *buffer;
	struct binder_buffer *kern_ptr;

	kern_ptr = user_ptr - proc->user_buffer_offset
		- offsetof(struct binder_buffer, data);

	while (n) {
		buffer = rb_entry(n, struct binder_buffer, rb_node);
		BUG_ON(buffer->free);

		if (kern_ptr < buffer)
			n = n->rb_left;
		else if (kern_ptr > buffer)
			n = n->rb_right;
		else
			return buffer;
	}
	return NULL;
}

// 为(end - start)这段内核虚拟内存分配/释放(allocate == 1)物理内存页，并将这些物理页
// 关联到用户虚拟内存(*vma)
static int binder_update_page_range(struct binder_proc *proc, int allocate,
				    void *start, void *end,
				    struct vm_area_struct *vma)
{
	void *page_addr;
	unsigned long user_page_addr;
	struct vm_struct tmp_area;
	struct page **page;
	struct mm_struct *mm;

	binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
		     "%d: %s pages %p-%p\n", proc->pid,
		     allocate ? "allocate" : "free", start, end);

	if (end <= start)
		return 0;

	trace_binder_update_page_range(proc, allocate, start, end);

	if (vma)
		mm = NULL;
	else
		mm = get_task_mm(proc->tsk);

	if (mm) {
		down_write(&mm->mmap_sem);
		vma = proc->vma;
		if (vma && mm != proc->vma_vm_mm) {
			pr_err("%d: vma mm and task mm mismatch\n",
				proc->pid);
			vma = NULL;
		}
	}

	if (allocate == 0)
		goto free_range;

	if (vma == NULL) {
		pr_err("%d: binder_alloc_buf failed to map pages in userspace, no vma\n",
			proc->pid);
		goto err_no_vma;
	}

	// 操作proc->pages[x]，为其赋值
	for (page_addr = start; page_addr < end; page_addr += PAGE_SIZE) {
		int ret;
		struct page **page_array_ptr;
		page = &proc->pages[(page_addr - proc->buffer) / PAGE_SIZE];

		BUG_ON(*page);
		*page = alloc_page(GFP_KERNEL | __GFP_HIGHMEM | __GFP_ZERO);
		if (*page == NULL) {
			pr_err("%d: binder_alloc_buf failed for page at %p\n",
				proc->pid, page_addr);
			goto err_alloc_page_failed;
		}
		tmp_area.addr = page_addr;
		tmp_area.size = PAGE_SIZE + PAGE_SIZE /* guard page? */;
		page_array_ptr = page;
		ret = map_vm_area(&tmp_area, PAGE_KERNEL, &page_array_ptr);
		if (ret) {
			pr_err("%d: binder_alloc_buf failed to map page at %p in kernel\n",
			       proc->pid, page_addr);
			goto err_map_kernel_failed;
		}
		user_page_addr =
			(uintptr_t)page_addr + proc->user_buffer_offset;
		ret = vm_insert_page(vma, user_page_addr, page[0]);
		if (ret) {
			pr_err("%d: binder_alloc_buf failed to map page at %lx in userspace\n",
			       proc->pid, user_page_addr);
			goto err_vm_insert_page_failed;
		}
		/* vm_insert_page does not seem to increment the refcount */
	}
	if (mm) {
		up_write(&mm->mmap_sem);
		mmput(mm);
	}
	return 0;

free_range:
	for (page_addr = end - PAGE_SIZE; page_addr >= start;
	     page_addr -= PAGE_SIZE) {
		page = &proc->pages[(page_addr - proc->buffer) / PAGE_SIZE];
		if (vma)
			zap_page_range(vma, (uintptr_t)page_addr +
				proc->user_buffer_offset, PAGE_SIZE, NULL);
err_vm_insert_page_failed:
		unmap_kernel_range((unsigned long)page_addr, PAGE_SIZE);
err_map_kernel_failed:
		__free_page(*page);
		*page = NULL;
err_alloc_page_failed:
		;
	}
err_no_vma:
	if (mm) {
		up_write(&mm->mmap_sem);
		mmput(mm);
	}
	return -ENOMEM;
}

static struct binder_buffer *binder_alloc_buf(struct binder_proc *proc,
					      size_t data_size,
					      size_t offsets_size, int is_async)
{
	struct rb_node *n = proc->free_buffers.rb_node;
	struct binder_buffer *buffer;
	size_t buffer_size;
	struct rb_node *best_fit = NULL;
	void *has_page_addr;
	void *end_page_addr;
	size_t size;

	if (proc->vma == NULL) {
		pr_err("%d: binder_alloc_buf, no vma\n",
		       proc->pid);
		return NULL;
	}

	size = ALIGN(data_size, sizeof(void *)) +
		ALIGN(offsets_size, sizeof(void *));

	if (size < data_size || size < offsets_size) {
		binder_user_error("%d: got transaction with invalid size %zd-%zd\n",
				proc->pid, data_size, offsets_size);
		return NULL;
	}

	if (is_async &&
	    proc->free_async_space < size + sizeof(struct binder_buffer)) {
		binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
			     "%d: binder_alloc_buf size %zd failed, no async space left\n",
			      proc->pid, size);
		return NULL;
	}

	while (n) {
		buffer = rb_entry(n, struct binder_buffer, rb_node);
		BUG_ON(!buffer->free);
		buffer_size = binder_buffer_size(proc, buffer);

		if (size < buffer_size) {
			best_fit = n;
			n = n->rb_left;
		} else if (size > buffer_size)
			n = n->rb_right;
		else {
			best_fit = n;
			break;
		}
	}
	if (best_fit == NULL) {
		pr_err("%d: binder_alloc_buf size %zd failed, no address space\n",
			proc->pid, size);
		return NULL;
	}
	if (n == NULL) {
		buffer = rb_entry(best_fit, struct binder_buffer, rb_node);
		buffer_size = binder_buffer_size(proc, buffer);
	}

	binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
		     "%d: binder_alloc_buf size %zd got buffer %p size %zd\n",
		      proc->pid, size, buffer, buffer_size);

	has_page_addr =
		(void *)(((uintptr_t)buffer->data + buffer_size) & PAGE_MASK);
	if (n == NULL) {
		if (size + sizeof(struct binder_buffer) + 4 >= buffer_size)
			buffer_size = size; /* no room for other buffers */
		else
			buffer_size = size + sizeof(struct binder_buffer);
	}
	end_page_addr =
		(void *)PAGE_ALIGN((uintptr_t)buffer->data + buffer_size);
	if (end_page_addr > has_page_addr)
		end_page_addr = has_page_addr;
	if (binder_update_page_range(proc, 1,
	    (void *)PAGE_ALIGN((uintptr_t)buffer->data), end_page_addr, NULL))
		return NULL;

	rb_erase(best_fit, &proc->free_buffers);
	buffer->free = 0;
	binder_insert_allocated_buffer(proc, buffer);
	if (buffer_size != size) {
		struct binder_buffer *new_buffer = (void *)buffer->data + size;
		list_add(&new_buffer->entry, &buffer->entry);
		new_buffer->free = 1;
		binder_insert_free_buffer(proc, new_buffer);
	}
	binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
		     "%d: binder_alloc_buf size %zd got %p\n",
		      proc->pid, size, buffer);
	buffer->data_size = data_size;
	buffer->offsets_size = offsets_size;
	buffer->async_transaction = is_async;
	if (is_async) {
		proc->free_async_space -= size + sizeof(struct binder_buffer);
		binder_debug(BINDER_DEBUG_BUFFER_ALLOC_ASYNC,
			     "%d: binder_alloc_buf size %zd async free %zd\n",
			      proc->pid, size, proc->free_async_space);
	}

	return buffer;
}

static void *buffer_start_page(struct binder_buffer *buffer)
{
	return (void *)((uintptr_t)buffer & PAGE_MASK);
}

static void *buffer_end_page(struct binder_buffer *buffer)
{
	return (void *)(((uintptr_t)(buffer + 1) - 1) & PAGE_MASK);
}

static void binder_delete_free_buffer(struct binder_proc *proc,
				      struct binder_buffer *buffer)
{
	struct binder_buffer *prev, *next = NULL;
	int free_page_end = 1;
	int free_page_start = 1;

	BUG_ON(proc->buffers.next == &buffer->entry);
	prev = list_entry(buffer->entry.prev, struct binder_buffer, entry);
	BUG_ON(!prev->free);
	if (buffer_end_page(prev) == buffer_start_page(buffer)) {
		free_page_start = 0;
		if (buffer_end_page(prev) == buffer_end_page(buffer))
			free_page_end = 0;
		binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
			     "%d: merge free, buffer %p share page with %p\n",
			      proc->pid, buffer, prev);
	}

	if (!list_is_last(&buffer->entry, &proc->buffers)) {
		next = list_entry(buffer->entry.next,
				  struct binder_buffer, entry);
		if (buffer_start_page(next) == buffer_end_page(buffer)) {
			free_page_end = 0;
			if (buffer_start_page(next) ==
			    buffer_start_page(buffer))
				free_page_start = 0;
			binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
				     "%d: merge free, buffer %p share page with %p\n",
				      proc->pid, buffer, prev);
		}
	}
	list_del(&buffer->entry);
	if (free_page_start || free_page_end) {
		binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
			     "%d: merge free, buffer %p do not share page%s%s with with %p or %p\n",
			     proc->pid, buffer, free_page_start ? "" : " end",
			     free_page_end ? "" : " start", prev, next);
		binder_update_page_range(proc, 0, free_page_start ?
			buffer_start_page(buffer) : buffer_end_page(buffer),
			(free_page_end ? buffer_end_page(buffer) :
			buffer_start_page(buffer)) + PAGE_SIZE, NULL);
	}
}

static void binder_free_buf(struct binder_proc *proc,
			    struct binder_buffer *buffer)
{
	size_t size, buffer_size;

	buffer_size = binder_buffer_size(proc, buffer);

	size = ALIGN(buffer->data_size, sizeof(void *)) +
		ALIGN(buffer->offsets_size, sizeof(void *));

	binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
		     "%d: binder_free_buf %p size %zd buffer_size %zd\n",
		      proc->pid, buffer, size, buffer_size);

	BUG_ON(buffer->free);
	BUG_ON(size > buffer_size);
	BUG_ON(buffer->transaction != NULL);
	BUG_ON((void *)buffer < proc->buffer);
	BUG_ON((void *)buffer > proc->buffer + proc->buffer_size);

	if (buffer->async_transaction) {
		proc->free_async_space += size + sizeof(struct binder_buffer);

		binder_debug(BINDER_DEBUG_BUFFER_ALLOC_ASYNC,
			     "%d: binder_free_buf size %zd async free %zd\n",
			      proc->pid, size, proc->free_async_space);
	}

	binder_update_page_range(proc, 0,
		(void *)PAGE_ALIGN((uintptr_t)buffer->data),
		(void *)(((uintptr_t)buffer->data + buffer_size) & PAGE_MASK),
		NULL);
	rb_erase(&buffer->rb_node, &proc->allocated_buffers);
	buffer->free = 1;
	if (!list_is_last(&buffer->entry, &proc->buffers)) {
		struct binder_buffer *next = list_entry(buffer->entry.next,
						struct binder_buffer, entry);
		if (next->free) {
			rb_erase(&next->rb_node, &proc->free_buffers);
			binder_delete_free_buffer(proc, next);
		}
	}
	if (proc->buffers.next != &buffer->entry) {
		struct binder_buffer *prev = list_entry(buffer->entry.prev,
						struct binder_buffer, entry);
		if (prev->free) {
			binder_delete_free_buffer(proc, buffer);
			rb_erase(&prev->rb_node, &proc->free_buffers);
			buffer = prev;
		}
	}
	binder_insert_free_buffer(proc, buffer);
}

static struct binder_node *binder_get_node(struct binder_proc *proc,
					   void __user *ptr)
{
	struct rb_node *n = proc->nodes.rb_node;
	struct binder_node *node;

	while (n) {
		node = rb_entry(n, struct binder_node, rb_node);

		if (ptr < node->ptr)
			n = n->rb_left;
		else if (ptr > node->ptr)
			n = n->rb_right;
		else
			return node;
	}
	return NULL;
}

/* 从struct binder_proc的nodes红黑树中，根据ptr寻找binder_node对象，
   如果找不到就创建一个新的。
   参数:
   		proc  : process object
   		ptr   : 实际对象的弱引用对象(RefBase::weakref_type*)
   		cookie: 实际对象的指针
   参数由来，相见addService()流程中的writeStrongBinder()(Parcel.cpp)

   只创建node对象，并不操作引用计数
 */
static struct binder_node *binder_new_node(struct binder_proc *proc,
					   void __user *ptr,
					   void __user *cookie)
{
	struct rb_node **p = &proc->nodes.rb_node;
	struct rb_node *parent = NULL;
	struct binder_node *node;

	while (*p) {
		parent = *p;
		node = rb_entry(parent, struct binder_node, rb_node);

		if (ptr < node->ptr)
			p = &(*p)->rb_left;
		else if (ptr > node->ptr)
			p = &(*p)->rb_right;
		else
			return NULL;
	}

	node = kzalloc(sizeof(*node), GFP_KERNEL);
	if (node == NULL)
		return NULL;
	binder_stats_created(BINDER_STAT_NODE);
	rb_link_node(&node->rb_node, parent, p);
	rb_insert_color(&node->rb_node, &proc->nodes);
	node->debug_id = ++binder_last_id;
	node->proc = proc;
	node->ptr = ptr;
	node->cookie = cookie;
	node->work.type = BINDER_WORK_NODE;
	INIT_LIST_HEAD(&node->work.entry);
	INIT_LIST_HEAD(&node->async_todo);
	binder_debug(BINDER_DEBUG_INTERNAL_REFS,
		     "%d:%d node %d u%p c%p created\n",
		     proc->pid, current->pid, node->debug_id,
		     node->ptr, node->cookie);
	return node;
}

static int binder_inc_node(struct binder_node *node, int strong, int internal,
			   struct list_head *target_list)
{
	if (strong) {
		if (internal) {
			if (target_list == NULL &&
			    node->internal_strong_refs == 0 &&
			    !(node == binder_context_mgr_node &&
			    node->has_strong_ref)) {
				pr_err("invalid inc strong node for %d\n",
					node->debug_id);
				return -EINVAL;
			}
			node->internal_strong_refs++;
		} else
			node->local_strong_refs++;
		if (!node->has_strong_ref && target_list) {
			list_del_init(&node->work.entry);
			list_add_tail(&node->work.entry, target_list);
		}
	} else {
		if (!internal)
			node->local_weak_refs++;
		if (!node->has_weak_ref && list_empty(&node->work.entry)) {
			if (target_list == NULL) {
				pr_err("invalid inc weak node for %d\n",
					node->debug_id);
				return -EINVAL;
			}
			list_add_tail(&node->work.entry, target_list);
		}
	}
	return 0;
}

static int binder_dec_node(struct binder_node *node, int strong, int internal)
{
	if (strong) {
		if (internal)
			node->internal_strong_refs--;
		else
			node->local_strong_refs--;
		if (node->local_strong_refs || node->internal_strong_refs)
			return 0;
	} else {
		if (!internal)
			node->local_weak_refs--;
		if (node->local_weak_refs || !hlist_empty(&node->refs))
			return 0;
	}
	if (node->proc && (node->has_strong_ref || node->has_weak_ref)) {
		if (list_empty(&node->work.entry)) {
			list_add_tail(&node->work.entry, &node->proc->todo);
			wake_up_interruptible(&node->proc->wait);
		}
	} else {
		if (hlist_empty(&node->refs) && !node->local_strong_refs &&
		    !node->local_weak_refs) {
			list_del_init(&node->work.entry);
			if (node->proc) {
				rb_erase(&node->rb_node, &node->proc->nodes);
				binder_debug(BINDER_DEBUG_INTERNAL_REFS,
					     "refless node %d deleted\n",
					     node->debug_id);
			} else {
				hlist_del(&node->dead_node);
				binder_debug(BINDER_DEBUG_INTERNAL_REFS,
					     "dead node %d deleted\n",
					     node->debug_id);
			}
			kfree(node);
			binder_stats_deleted(BINDER_STAT_NODE);
		}
	}

	return 0;
}


static struct binder_ref *binder_get_ref(struct binder_proc *proc,
					 uint32_t desc)
{
	struct rb_node *n = proc->refs_by_desc.rb_node;
	struct binder_ref *ref;

	while (n) {
		ref = rb_entry(n, struct binder_ref, rb_node_desc);

		if (desc < ref->desc)
			n = n->rb_left;
		else if (desc > ref->desc)
			n = n->rb_right;
		else
			return ref;
	}
	return NULL;
}

// 为node创建一个ref
static struct binder_ref *binder_get_ref_for_node(struct binder_proc *proc,
						  struct binder_node *node)
{
	struct rb_node *n;
	struct rb_node **p = &proc->refs_by_node.rb_node;
	struct rb_node *parent = NULL;
	struct binder_ref *ref, *new_ref;

	// 先根据node的内存地址进行查找
	while (*p) {
		parent = *p;
		ref = rb_entry(parent, struct binder_ref, rb_node_node);

		if (node < ref->node)
			p = &(*p)->rb_left;
		else if (node > ref->node)
			p = &(*p)->rb_right;
		else
			return ref;
	}
	// 找不到就新建一个ref，并加入根据内存地址进行索引的rb tree中
	new_ref = kzalloc(sizeof(*ref), GFP_KERNEL);
	if (new_ref == NULL)
		return NULL;
	binder_stats_created(BINDER_STAT_REF);
	new_ref->debug_id = ++binder_last_id;
	new_ref->proc = proc;
	new_ref->node = node;
	rb_link_node(&new_ref->rb_node_node, parent, p);
	rb_insert_color(&new_ref->rb_node_node, &proc->refs_by_node);

	// 为新的ref生成desc值，其实就是当前node个数
	new_ref->desc = (node == binder_context_mgr_node) ? 0 : 1;
	for (n = rb_first(&proc->refs_by_desc); n != NULL; n = rb_next(n)) {
		ref = rb_entry(n, struct binder_ref, rb_node_desc);
		if (ref->desc > new_ref->desc)
			break;
		new_ref->desc = ref->desc + 1;
	}

	// 并将其加入根据desc进行索引的rb tree中
	p = &proc->refs_by_desc.rb_node;
	while (*p) {
		parent = *p;
		ref = rb_entry(parent, struct binder_ref, rb_node_desc);

		if (new_ref->desc < ref->desc)
			p = &(*p)->rb_left;
		else if (new_ref->desc > ref->desc)
			p = &(*p)->rb_right;
		else
			BUG();
	}
	rb_link_node(&new_ref->rb_node_desc, parent, p);
	rb_insert_color(&new_ref->rb_node_desc, &proc->refs_by_desc);

	// 最后将ref加入其所引用的node的refs list中
	if (node) {
		hlist_add_head(&new_ref->node_entry, &node->refs);

		binder_debug(BINDER_DEBUG_INTERNAL_REFS,
			     "%d new ref %d desc %d for node %d\n",
			      proc->pid, new_ref->debug_id, new_ref->desc,
			      node->debug_id);
	} else {
		binder_debug(BINDER_DEBUG_INTERNAL_REFS,
			     "%d new ref %d desc %d for dead node\n",
			      proc->pid, new_ref->debug_id, new_ref->desc);
	}
	return new_ref;
}

static void binder_delete_ref(struct binder_ref *ref)
{
	binder_debug(BINDER_DEBUG_INTERNAL_REFS,
		     "%d delete ref %d desc %d for node %d\n",
		      ref->proc->pid, ref->debug_id, ref->desc,
		      ref->node->debug_id);

	rb_erase(&ref->rb_node_desc, &ref->proc->refs_by_desc);
	rb_erase(&ref->rb_node_node, &ref->proc->refs_by_node);
	if (ref->strong)
		binder_dec_node(ref->node, 1, 1);
	hlist_del(&ref->node_entry);
	binder_dec_node(ref->node, 0, 1);
	if (ref->death) {
		binder_debug(BINDER_DEBUG_DEAD_BINDER,
			     "%d delete ref %d desc %d has death notification\n",
			      ref->proc->pid, ref->debug_id, ref->desc);
		list_del(&ref->death->work.entry);
		kfree(ref->death);
		binder_stats_deleted(BINDER_STAT_DEATH);
	}
	kfree(ref);
	binder_stats_deleted(BINDER_STAT_REF);
}

static int binder_inc_ref(struct binder_ref *ref, int strong,
			  struct list_head *target_list)
{
	int ret;
	if (strong) {
		if (ref->strong == 0) {
			ret = binder_inc_node(ref->node, 1, 1, target_list);
			if (ret)
				return ret;
		}
		ref->strong++;
	} else {
		if (ref->weak == 0) {
			ret = binder_inc_node(ref->node, 0, 1, target_list);
			if (ret)
				return ret;
		}
		ref->weak++;
	}
	return 0;
}


static int binder_dec_ref(struct binder_ref *ref, int strong)
{
	if (strong) {
		if (ref->strong == 0) {
			binder_user_error("%d invalid dec strong, ref %d desc %d s %d w %d\n",
					  ref->proc->pid, ref->debug_id,
					  ref->desc, ref->strong, ref->weak);
			return -EINVAL;
		}
		ref->strong--;
		if (ref->strong == 0) {
			int ret;
			ret = binder_dec_node(ref->node, strong, 1);
			if (ret)
				return ret;
		}
	} else {
		if (ref->weak == 0) {
			binder_user_error("%d invalid dec weak, ref %d desc %d s %d w %d\n",
					  ref->proc->pid, ref->debug_id,
					  ref->desc, ref->strong, ref->weak);
			return -EINVAL;
		}
		ref->weak--;
	}
	if (ref->strong == 0 && ref->weak == 0)
		binder_delete_ref(ref);
	return 0;
}

static void binder_pop_transaction(struct binder_thread *target_thread,
				   struct binder_transaction *t)
{
	if (target_thread) {
		BUG_ON(target_thread->transaction_stack != t);
		BUG_ON(target_thread->transaction_stack->from != target_thread);
		target_thread->transaction_stack =
			target_thread->transaction_stack->from_parent;
		t->from = NULL;
	}
	t->need_reply = 0;
	if (t->buffer)
		t->buffer->transaction = NULL;
	kfree(t);
	binder_stats_deleted(BINDER_STAT_TRANSACTION);
}

static void binder_send_failed_reply(struct binder_transaction *t,
				     uint32_t error_code)
{
	struct binder_thread *target_thread;
	BUG_ON(t->flags & TF_ONE_WAY);
	while (1) {
		target_thread = t->from;
		if (target_thread) {
			if (target_thread->return_error != BR_OK &&
			   target_thread->return_error2 == BR_OK) {
				target_thread->return_error2 =
					target_thread->return_error;
				target_thread->return_error = BR_OK;
			}
			if (target_thread->return_error == BR_OK) {
				binder_debug(BINDER_DEBUG_FAILED_TRANSACTION,
					     "send failed reply for transaction %d to %d:%d\n",
					      t->debug_id, target_thread->proc->pid,
					      target_thread->pid);

				binder_pop_transaction(target_thread, t);
				target_thread->return_error = error_code;
				wake_up_interruptible(&target_thread->wait);
			} else {
				pr_err("reply failed, target thread, %d:%d, has error code %d already\n",
					target_thread->proc->pid,
					target_thread->pid,
					target_thread->return_error);
			}
			return;
		} else {
			struct binder_transaction *next = t->from_parent;

			binder_debug(BINDER_DEBUG_FAILED_TRANSACTION,
				     "send failed reply for transaction %d, target dead\n",
				     t->debug_id);

			binder_pop_transaction(target_thread, t);
			if (next == NULL) {
				binder_debug(BINDER_DEBUG_DEAD_BINDER,
					     "reply failed, no target thread at root\n");
				return;
			}
			t = next;
			binder_debug(BINDER_DEBUG_DEAD_BINDER,
				     "reply failed, no target thread -- retry %d\n",
				      t->debug_id);
		}
	}
}

static void binder_transaction_buffer_release(struct binder_proc *proc,
					      struct binder_buffer *buffer,
					      size_t *failed_at)
{
	size_t *offp, *off_end;
	int debug_id = buffer->debug_id;

	binder_debug(BINDER_DEBUG_TRANSACTION,
		     "%d buffer release %d, size %zd-%zd, failed at %p\n",
		     proc->pid, buffer->debug_id,
		     buffer->data_size, buffer->offsets_size, failed_at);

	if (buffer->target_node)
		binder_dec_node(buffer->target_node, 1, 0);

	offp = (size_t *)(buffer->data + ALIGN(buffer->data_size, sizeof(void *)));
	if (failed_at)
		off_end = failed_at;
	else
		off_end = (void *)offp + buffer->offsets_size;
	for (; offp < off_end; offp++) {
		struct flat_binder_object *fp;
		if (*offp > buffer->data_size - sizeof(*fp) ||
		    buffer->data_size < sizeof(*fp) ||
		    !IS_ALIGNED(*offp, sizeof(void *))) {
			pr_err("transaction release %d bad offset %zd, size %zd\n",
			 debug_id, *offp, buffer->data_size);
			continue;
		}
		fp = (struct flat_binder_object *)(buffer->data + *offp);
		switch (fp->type) {
		case BINDER_TYPE_BINDER:
		case BINDER_TYPE_WEAK_BINDER: {
			struct binder_node *node = binder_get_node(proc, fp->binder);
			if (node == NULL) {
				pr_err("transaction release %d bad node %p\n",
					debug_id, fp->binder);
				break;
			}
			binder_debug(BINDER_DEBUG_TRANSACTION,
				     "        node %d u%p\n",
				     node->debug_id, node->ptr);
			binder_dec_node(node, fp->type == BINDER_TYPE_BINDER, 0);
		} break;
		case BINDER_TYPE_HANDLE:
		case BINDER_TYPE_WEAK_HANDLE: {
			struct binder_ref *ref = binder_get_ref(proc, fp->handle);
			if (ref == NULL) {
				pr_err("transaction release %d bad handle %ld\n",
				 debug_id, fp->handle);
				break;
			}
			binder_debug(BINDER_DEBUG_TRANSACTION,
				     "        ref %d desc %d (node %d)\n",
				     ref->debug_id, ref->desc, ref->node->debug_id);
			binder_dec_ref(ref, fp->type == BINDER_TYPE_HANDLE);
		} break;

		case BINDER_TYPE_FD:
			binder_debug(BINDER_DEBUG_TRANSACTION,
				     "        fd %ld\n", fp->handle);
			if (failed_at)
				task_close_fd(proc, fp->handle);
			break;

		default:
			pr_err("transaction release %d bad object type %lx\n",
				debug_id, fp->type);
			break;
		}
	}
}

static void binder_transaction(struct binder_proc *proc,
			       struct binder_thread *thread,
			       struct binder_transaction_data *tr, int reply)
{
	struct binder_transaction *t;
	struct binder_work *tcomplete;
	size_t *offp, *off_end;
	struct binder_proc *target_proc;
	struct binder_thread *target_thread = NULL;
	struct binder_node *target_node = NULL;
	struct list_head *target_list;
	wait_queue_head_t *target_wait;
	struct binder_transaction *in_reply_to = NULL;
	struct binder_transaction_log_entry *e;
	uint32_t return_error;

	e = binder_transaction_log_add(&binder_transaction_log);
	e->call_type = reply ? 2 : !!(tr->flags & TF_ONE_WAY);
	e->from_proc = proc->pid;
	e->from_thread = thread->pid;
	e->target_handle = tr->target.handle;
	e->data_size = tr->data_size;
	e->offsets_size = tr->offsets_size;

	if (reply) {
		in_reply_to = thread->transaction_stack;
		if (in_reply_to == NULL) {
			binder_user_error("%d:%d got reply transaction with no transaction stack\n",
					  proc->pid, thread->pid);
			return_error = BR_FAILED_REPLY;
			goto err_empty_call_stack;
		}
		binder_set_nice(in_reply_to->saved_priority);
		if (in_reply_to->to_thread != thread) {
			binder_user_error("%d:%d got reply transaction with bad transaction stack, transaction %d has target %d:%d\n",
				proc->pid, thread->pid, in_reply_to->debug_id,
				in_reply_to->to_proc ?
				in_reply_to->to_proc->pid : 0,
				in_reply_to->to_thread ?
				in_reply_to->to_thread->pid : 0);
			return_error = BR_FAILED_REPLY;
			in_reply_to = NULL;
			goto err_bad_call_stack;
		}
		thread->transaction_stack = in_reply_to->to_parent;
		target_thread = in_reply_to->from;
		if (target_thread == NULL) {
			return_error = BR_DEAD_REPLY;
			goto err_dead_binder;
		}
		if (target_thread->transaction_stack != in_reply_to) {
			binder_user_error("%d:%d got reply transaction with bad target transaction stack %d, expected %d\n",
				proc->pid, thread->pid,
				target_thread->transaction_stack ?
				target_thread->transaction_stack->debug_id : 0,
				in_reply_to->debug_id);
			return_error = BR_FAILED_REPLY;
			in_reply_to = NULL;
			target_thread = NULL;
			goto err_dead_binder;
		}
		target_proc = target_thread->proc;
	} else {
		if (tr->target.handle) {
			struct binder_ref *ref;
			ref = binder_get_ref(proc, tr->target.handle);
			if (ref == NULL) {
				binder_user_error("%d:%d got transaction to invalid handle\n",
					proc->pid, thread->pid);
				return_error = BR_FAILED_REPLY;
				goto err_invalid_target_handle;
			}
			target_node = ref->node;
		} else {
			target_node = binder_context_mgr_node;
			if (target_node == NULL) {
				return_error = BR_DEAD_REPLY;
				goto err_no_context_mgr_node;
			}
		}
		e->to_node = target_node->debug_id;
		target_proc = target_node->proc;
		if (target_proc == NULL) {
			return_error = BR_DEAD_REPLY;
			goto err_dead_binder;
		}

		// 给我们的transaction找一个合适的thread，target node是确定的，target proc也是确定的，
		// 但是target thread却不确定，找一个合适的，最空闲的?!
		if (!(tr->flags & TF_ONE_WAY) && thread->transaction_stack) {
			struct binder_transaction *tmp;
			tmp = thread->transaction_stack;
			if (tmp->to_thread != thread) {
				binder_user_error("%d:%d got new transaction with bad transaction stack, transaction %d has target %d:%d\n",
					proc->pid, thread->pid, tmp->debug_id,
					tmp->to_proc ? tmp->to_proc->pid : 0,
					tmp->to_thread ?
					tmp->to_thread->pid : 0);
				return_error = BR_FAILED_REPLY;
				goto err_bad_call_stack;
			}
			while (tmp) {
				if (tmp->from && tmp->from->proc == target_proc)
					target_thread = tmp->from;
				tmp = tmp->from_parent;
			}
		}
	}
	// 如果有target thread就交给它处理，否则就交给target proc处理
	if (target_thread) {
		e->to_thread = target_thread->pid;
		target_list = &target_thread->todo;
		target_wait = &target_thread->wait;
	} else {
		target_list = &target_proc->todo;
		target_wait = &target_proc->wait;
	}
	e->to_proc = target_proc->pid;

	// 创建transaction对象，用于挂载到目标端的transaction stack上
	/* TODO: reuse incoming transaction for reply */
	t = kzalloc(sizeof(*t), GFP_KERNEL);
	if (t == NULL) {
		return_error = BR_FAILED_REPLY;
		goto err_alloc_t_failed;
	}
	binder_stats_created(BINDER_STAT_TRANSACTION);

	// 创建work对象，用于给发起端反馈: transaction complete
	tcomplete = kzalloc(sizeof(*tcomplete), GFP_KERNEL);
	if (tcomplete == NULL) {
		return_error = BR_FAILED_REPLY;
		goto err_alloc_tcomplete_failed;
	}
	binder_stats_created(BINDER_STAT_TRANSACTION_COMPLETE);

	t->debug_id = ++binder_last_id;
	e->debug_id = t->debug_id;

	if (reply)
		binder_debug(BINDER_DEBUG_TRANSACTION,
			     "%d:%d BC_REPLY %d -> %d:%d, data %p-%p size %zd-%zd\n",
			     proc->pid, thread->pid, t->debug_id,
			     target_proc->pid, target_thread->pid,
			     tr->data.ptr.buffer, tr->data.ptr.offsets,
			     tr->data_size, tr->offsets_size);
	else
		binder_debug(BINDER_DEBUG_TRANSACTION,
			     "%d:%d BC_TRANSACTION %d -> %d - node %d, data %p-%p size %zd-%zd\n",
			     proc->pid, thread->pid, t->debug_id,
			     target_proc->pid, target_node->debug_id,
			     tr->data.ptr.buffer, tr->data.ptr.offsets,
			     tr->data_size, tr->offsets_size);

	if (!reply && !(tr->flags & TF_ONE_WAY))
		t->from = thread;
	else
		t->from = NULL;
	t->sender_euid = proc->tsk->cred->euid;
	t->to_proc = target_proc;
	t->to_thread = target_thread;
	t->code = tr->code;
	t->flags = tr->flags;
	t->priority = task_nice(current);

	trace_binder_transaction(reply, t, target_node);

	// 从目标端进程buffer中申请空间
	t->buffer = binder_alloc_buf(target_proc, tr->data_size,
		tr->offsets_size, !reply && (t->flags & TF_ONE_WAY)); // async? Two-way transaction是sync的，否则就是async的
	if (t->buffer == NULL) {
		return_error = BR_FAILED_REPLY;
		goto err_binder_alloc_buf_failed;
	}
	t->buffer->allow_user_free = 0;
	t->buffer->debug_id = t->debug_id;
	t->buffer->transaction = t; // buffer, transaction关联
	t->buffer->target_node = target_node;
	trace_binder_transaction_alloc_buf(t->buffer);
	if (target_node)
		binder_inc_node(target_node, 1, 0, NULL);

	offp = (size_t *)(t->buffer->data + ALIGN(tr->data_size, sizeof(void *)));

	// 从发起端进程复制数据到接收端进程，其中可能包含flat_binder_object对象
	if (copy_from_user(t->buffer->data, tr->data.ptr.buffer, tr->data_size)) {
		binder_user_error("%d:%d got transaction with invalid data ptr\n",
				proc->pid, thread->pid);
		return_error = BR_FAILED_REPLY;
		goto err_copy_data_failed;
	}
	// 复制offset数据
	if (copy_from_user(offp, tr->data.ptr.offsets, tr->offsets_size)) {
		binder_user_error("%d:%d got transaction with invalid offsets ptr\n",
				proc->pid, thread->pid);
		return_error = BR_FAILED_REPLY;
		goto err_copy_data_failed;
	}
	if (!IS_ALIGNED(tr->offsets_size, sizeof(size_t))) {
		binder_user_error("%d:%d got transaction with invalid offsets size, %zd\n",
				proc->pid, thread->pid, tr->offsets_size);
		return_error = BR_FAILED_REPLY;
		goto err_bad_offset;
	}
	off_end = (void *)offp + tr->offsets_size;

	// 逐个处理flat_binder_object对象，如有必要创建node对象，比如service注册时提供的service对象，就是在此时创建的node
	for (; offp < off_end; offp++) {
		struct flat_binder_object *fp;
		if (*offp > t->buffer->data_size - sizeof(*fp) ||
		    t->buffer->data_size < sizeof(*fp) ||
		    !IS_ALIGNED(*offp, sizeof(void *))) {
			binder_user_error("%d:%d got transaction with invalid offset, %zd\n",
					proc->pid, thread->pid, *offp);
			return_error = BR_FAILED_REPLY;
			goto err_bad_offset;
		}
		fp = (struct flat_binder_object *)(t->buffer->data + *offp);

		// 根据类型，处理flat_binder_object对象
		switch (fp->type) {
		case BINDER_TYPE_BINDER:
		case BINDER_TYPE_WEAK_BINDER: {
			struct binder_ref *ref;
			// fp->binder实际是service对象的weak ref对象，见addService()流程
			struct binder_node *node = binder_get_node(proc, fp->binder);
			// 如果没有就创建一个新的，service对象的node就是在此时创建的
			if (node == NULL) {
				node = binder_new_node(proc, fp->binder, fp->cookie);
				if (node == NULL) {
					return_error = BR_FAILED_REPLY;
					goto err_binder_new_node_failed;
				}
				node->min_priority = fp->flags & FLAT_BINDER_FLAG_PRIORITY_MASK;
				node->accept_fds = !!(fp->flags & FLAT_BINDER_FLAG_ACCEPTS_FDS);
			}
			if (fp->cookie != node->cookie) {
				binder_user_error("%d:%d sending u%p node %d, cookie mismatch %p != %p\n",
					proc->pid, thread->pid,
					fp->binder, node->debug_id,
					fp->cookie, node->cookie);
				goto err_binder_get_ref_for_node_failed;
			}
			// 创建node的ref对象
			ref = binder_get_ref_for_node(target_proc, node);
			if (ref == NULL) {
				return_error = BR_FAILED_REPLY;
				goto err_binder_get_ref_for_node_failed;
			}
			/*
				这是binder最为核心的地方，即，
				在server进程，flat_binder_object标示一个实实在在的对象，
				而进入client进程或者service mgr进程，它则变成一个handle，
				这之间的变换由driver来转换，就是这里。
			 */
			if (fp->type == BINDER_TYPE_BINDER)
				fp->type = BINDER_TYPE_HANDLE;
			else
				fp->type = BINDER_TYPE_WEAK_HANDLE;
			
			fp->handle = ref->desc; // handle的由来，原来就是指向node的ref的desc值，而不是mgr生成的值
			binder_inc_ref(ref, fp->type == BINDER_TYPE_HANDLE,
				       &thread->todo);

			trace_binder_transaction_node_to_ref(t, node, ref);
			binder_debug(BINDER_DEBUG_TRANSACTION,
				     "        node %d u%p -> ref %d desc %d\n",
				     node->debug_id, node->ptr, ref->debug_id,
				     ref->desc);
		} break;
		case BINDER_TYPE_HANDLE:
		case BINDER_TYPE_WEAK_HANDLE: {
			struct binder_ref *ref = binder_get_ref(proc, fp->handle);
			if (ref == NULL) {
				binder_user_error("%d:%d got transaction with invalid handle, %ld\n",
						proc->pid,
						thread->pid, fp->handle);
				return_error = BR_FAILED_REPLY;
				goto err_binder_get_ref_failed;
			}
			if (ref->node->proc == target_proc) {
				if (fp->type == BINDER_TYPE_HANDLE)
					fp->type = BINDER_TYPE_BINDER;
				else
					fp->type = BINDER_TYPE_WEAK_BINDER;
				fp->binder = ref->node->ptr;
				fp->cookie = ref->node->cookie;
				binder_inc_node(ref->node, fp->type == BINDER_TYPE_BINDER, 0, NULL);
				trace_binder_transaction_ref_to_node(t, ref);
				binder_debug(BINDER_DEBUG_TRANSACTION,
					     "        ref %d desc %d -> node %d u%p\n",
					     ref->debug_id, ref->desc, ref->node->debug_id,
					     ref->node->ptr);
			} else {
				struct binder_ref *new_ref;
				new_ref = binder_get_ref_for_node(target_proc, ref->node);
				if (new_ref == NULL) {
					return_error = BR_FAILED_REPLY;
					goto err_binder_get_ref_for_node_failed;
				}
				fp->handle = new_ref->desc;
				binder_inc_ref(new_ref, fp->type == BINDER_TYPE_HANDLE, NULL);
				trace_binder_transaction_ref_to_ref(t, ref,
								    new_ref);
				binder_debug(BINDER_DEBUG_TRANSACTION,
					     "        ref %d desc %d -> ref %d desc %d (node %d)\n",
					     ref->debug_id, ref->desc, new_ref->debug_id,
					     new_ref->desc, ref->node->debug_id);
			}
		} break;

		case BINDER_TYPE_FD: {
			int target_fd;
			struct file *file;

			if (reply) {
				if (!(in_reply_to->flags & TF_ACCEPT_FDS)) {
					binder_user_error("%d:%d got reply with fd, %ld, but target does not allow fds\n",
						proc->pid, thread->pid, fp->handle);
					return_error = BR_FAILED_REPLY;
					goto err_fd_not_allowed;
				}
			} else if (!target_node->accept_fds) {
				binder_user_error("%d:%d got transaction with fd, %ld, but target does not allow fds\n",
					proc->pid, thread->pid, fp->handle);
				return_error = BR_FAILED_REPLY;
				goto err_fd_not_allowed;
			}

			file = fget(fp->handle);
			if (file == NULL) {
				binder_user_error("%d:%d got transaction with invalid fd, %ld\n",
					proc->pid, thread->pid, fp->handle);
				return_error = BR_FAILED_REPLY;
				goto err_fget_failed;
			}
			target_fd = task_get_unused_fd_flags(target_proc, O_CLOEXEC);
			if (target_fd < 0) {
				fput(file);
				return_error = BR_FAILED_REPLY;
				goto err_get_unused_fd_failed;
			}
			task_fd_install(target_proc, target_fd, file);
			trace_binder_transaction_fd(t, fp->handle, target_fd);
			binder_debug(BINDER_DEBUG_TRANSACTION,
				     "        fd %ld -> %d\n", fp->handle, target_fd);
			/* TODO: fput? */
			fp->handle = target_fd;
		} break;

		default:
			binder_user_error("%d:%d got transaction with invalid object type, %lx\n",
				proc->pid, thread->pid, fp->type);
			return_error = BR_FAILED_REPLY;
			goto err_bad_object_type;
		}
	}
	
	if (reply) {
		BUG_ON(t->buffer->async_transaction != 0);
		binder_pop_transaction(target_thread, in_reply_to);
	} else if (!(t->flags & TF_ONE_WAY)) {
		BUG_ON(t->buffer->async_transaction != 0);
		t->need_reply = 1;
		t->from_parent = thread->transaction_stack;
		thread->transaction_stack = t;
	} else {
		BUG_ON(target_node == NULL);
		BUG_ON(t->buffer->async_transaction != 1);
		if (target_node->has_async_transaction) {
			target_list = &target_node->async_todo;
			target_wait = NULL;
		} else
			target_node->has_async_transaction = 1;
	}
	t->work.type = BINDER_WORK_TRANSACTION;
	list_add_tail(&t->work.entry, target_list); // 把transaction挂载到target list上，即目标线程或者目标进程的todo list上
	tcomplete->type = BINDER_WORK_TRANSACTION_COMPLETE;
	list_add_tail(&tcomplete->entry, &thread->todo); // 告诉发起transaction的进程，transaction complete
	if (target_wait)
		wake_up_interruptible(target_wait);
	return;

err_get_unused_fd_failed:
err_fget_failed:
err_fd_not_allowed:
err_binder_get_ref_for_node_failed:
err_binder_get_ref_failed:
err_binder_new_node_failed:
err_bad_object_type:
err_bad_offset:
err_copy_data_failed:
	trace_binder_transaction_failed_buffer_release(t->buffer);
	binder_transaction_buffer_release(target_proc, t->buffer, offp);
	t->buffer->transaction = NULL;
	binder_free_buf(target_proc, t->buffer);
err_binder_alloc_buf_failed:
	kfree(tcomplete);
	binder_stats_deleted(BINDER_STAT_TRANSACTION_COMPLETE);
err_alloc_tcomplete_failed:
	kfree(t);
	binder_stats_deleted(BINDER_STAT_TRANSACTION);
err_alloc_t_failed:
err_bad_call_stack:
err_empty_call_stack:
err_dead_binder:
err_invalid_target_handle:
err_no_context_mgr_node:
	binder_debug(BINDER_DEBUG_FAILED_TRANSACTION,
		     "%d:%d transaction failed %d, size %zd-%zd\n",
		     proc->pid, thread->pid, return_error,
		     tr->data_size, tr->offsets_size);

	{
		struct binder_transaction_log_entry *fe;
		fe = binder_transaction_log_add(&binder_transaction_log_failed);
		*fe = *e;
	}

	BUG_ON(thread->return_error != BR_OK);
	if (in_reply_to) {
		thread->return_error = BR_TRANSACTION_COMPLETE;
		binder_send_failed_reply(in_reply_to, return_error);
	} else
		thread->return_error = return_error;
}

// 处理user space发过来的指令
int binder_thread_write(struct binder_proc *proc, struct binder_thread *thread,
			void __user *buffer, int size, signed long *consumed)
{
	uint32_t cmd;
	void __user *ptr = buffer + *consumed;
	void __user *end = buffer + size;

	while (ptr < end && thread->return_error == BR_OK) {
		// 拿到command
		if (get_user(cmd, (uint32_t __user *)ptr))
			return -EFAULT;
		ptr += sizeof(uint32_t);
		trace_binder_command(cmd);
		if (_IOC_NR(cmd) < ARRAY_SIZE(binder_stats.bc)) {
			binder_stats.bc[_IOC_NR(cmd)]++;
			proc->stats.bc[_IOC_NR(cmd)]++;
			thread->stats.bc[_IOC_NR(cmd)]++;
		}
		// 按照cmd类型进行分类处理
		switch (cmd) {
		case BC_INCREFS:
		case BC_ACQUIRE:
		case BC_RELEASE:
		case BC_DECREFS: {
			// node相关的操作，即调整强弱指针引用计数
			uint32_t target; // 需要调整哪个binder_node
			struct binder_ref *ref; // 使用这个ref来索引target
			const char *debug_string;

			if (get_user(target, (uint32_t __user *)ptr))
				return -EFAULT;
			ptr += sizeof(uint32_t);
			if (target == 0 && binder_context_mgr_node &&
			    (cmd == BC_INCREFS || cmd == BC_ACQUIRE)) {
			    // 如果要找的target是context manager
				ref = binder_get_ref_for_node(proc, binder_context_mgr_node);
				if (ref->desc != target) {
					// 保证manager的desc一定是0，因为它是第一个node嘛
					binder_user_error("%d:%d tried to acquire reference to desc 0, got %d instead\n",
						proc->pid, thread->pid,
						ref->desc);
				}
			} else
				ref = binder_get_ref(proc, target);
			
			if (ref == NULL) {
				binder_user_error("%d:%d refcount change on invalid ref %d\n",
					proc->pid, thread->pid, target);
				break;
			}
			switch (cmd) {
			case BC_INCREFS:
				debug_string = "IncRefs";
				binder_inc_ref(ref, 0, NULL);
				break;
			case BC_ACQUIRE:
				debug_string = "Acquire";
				binder_inc_ref(ref, 1, NULL);
				break;
			case BC_RELEASE:
				debug_string = "Release";
				binder_dec_ref(ref, 1);
				break;
			case BC_DECREFS:
			default:
				debug_string = "DecRefs";
				binder_dec_ref(ref, 0);
				break;
			}
			binder_debug(BINDER_DEBUG_USER_REFS,
				     "%d:%d %s ref %d desc %d s %d w %d for node %d\n",
				     proc->pid, thread->pid, debug_string, ref->debug_id,
				     ref->desc, ref->strong, ref->weak, ref->node->debug_id);
			break;
		}
		case BC_INCREFS_DONE:
		case BC_ACQUIRE_DONE: {
			// 找到相关node，恢复其pending_xxx的值，pending_xxx标示kernel给目标user space发出inc/dec指令了，
			// 但是目标user space还没有返回inc/dec done指令，即kernel还不知道inc/dec是否成功。binder_thread_read()
			// 里面有相关操作。
			void __user *node_ptr;
			void *cookie;
			struct binder_node *node;

			if (get_user(node_ptr, (void * __user *)ptr))
				return -EFAULT;
			ptr += sizeof(void *);
			if (get_user(cookie, (void * __user *)ptr))
				return -EFAULT;
			ptr += sizeof(void *);
			node = binder_get_node(proc, node_ptr);
			if (node == NULL) {
				binder_user_error("%d:%d %s u%p no match\n",
					proc->pid, thread->pid,
					cmd == BC_INCREFS_DONE ?
					"BC_INCREFS_DONE" :
					"BC_ACQUIRE_DONE",
					node_ptr);
				break;
			}
			if (cookie != node->cookie) {
				binder_user_error("%d:%d %s u%p node %d cookie mismatch %p != %p\n",
					proc->pid, thread->pid,
					cmd == BC_INCREFS_DONE ?
					"BC_INCREFS_DONE" : "BC_ACQUIRE_DONE",
					node_ptr, node->debug_id,
					cookie, node->cookie);
				break;
			}
			if (cmd == BC_ACQUIRE_DONE) {
				if (node->pending_strong_ref == 0) {
					binder_user_error("%d:%d BC_ACQUIRE_DONE node %d has no pending acquire request\n",
						proc->pid, thread->pid,
						node->debug_id);
					break;
				}
				node->pending_strong_ref = 0;
			} else {
				if (node->pending_weak_ref == 0) {
					binder_user_error("%d:%d BC_INCREFS_DONE node %d has no pending increfs request\n",
						proc->pid, thread->pid,
						node->debug_id);
					break;
				}
				node->pending_weak_ref = 0;
			}
			binder_dec_node(node, cmd == BC_ACQUIRE_DONE, 0);
			binder_debug(BINDER_DEBUG_USER_REFS,
				     "%d:%d %s node %d ls %d lw %d\n",
				     proc->pid, thread->pid,
				     cmd == BC_INCREFS_DONE ? "BC_INCREFS_DONE" : "BC_ACQUIRE_DONE",
				     node->debug_id, node->local_strong_refs, node->local_weak_refs);
			break;
		}
		case BC_ATTEMPT_ACQUIRE:
			pr_err("BC_ATTEMPT_ACQUIRE not supported\n");
			return -EINVAL;
		case BC_ACQUIRE_RESULT:
			pr_err("BC_ACQUIRE_RESULT not supported\n");
			return -EINVAL;

		case BC_FREE_BUFFER: {
			// 1. 根据指针找到buffer; 2. 将其从环境中剥离出来; 3. 清理内存
			void __user *data_ptr;
			struct binder_buffer *buffer;

			if (get_user(data_ptr, (void * __user *)ptr))
				return -EFAULT;
			ptr += sizeof(void *);

			buffer = binder_buffer_lookup(proc, data_ptr);
			if (buffer == NULL) {
				binder_user_error("%d:%d BC_FREE_BUFFER u%p no match\n",
					proc->pid, thread->pid, data_ptr);
				break;
			}
			if (!buffer->allow_user_free) {
				binder_user_error("%d:%d BC_FREE_BUFFER u%p matched unreturned buffer\n",
					proc->pid, thread->pid, data_ptr);
				break;
			}
			binder_debug(BINDER_DEBUG_FREE_BUFFER,
				     "%d:%d BC_FREE_BUFFER u%p found buffer %d for %s transaction\n",
				     proc->pid, thread->pid, data_ptr, buffer->debug_id,
				     buffer->transaction ? "active" : "finished");

			// 如果buffer是用于某个transaction的，那么解除buffer和transaction之间的引用关系
			if (buffer->transaction) {
				buffer->transaction->buffer = NULL;
				buffer->transaction = NULL;
			}
			if (buffer->async_transaction && buffer->target_node) {
				BUG_ON(!buffer->target_node->has_async_transaction);
				if (list_empty(&buffer->target_node->async_todo))
					buffer->target_node->has_async_transaction = 0;
				else
					list_move_tail(buffer->target_node->async_todo.next, &thread->todo);
			}
			trace_binder_transaction_buffer_release(buffer);
			
			// 把buffer从环境中剥离出来之后，就是真正的清理工作了
			binder_transaction_buffer_release(proc, buffer, NULL);
			binder_free_buf(proc, buffer);
			break;
		}

		case BC_TRANSACTION:
		case BC_REPLY: {
			struct binder_transaction_data tr;

			if (copy_from_user(&tr, ptr, sizeof(tr)))
				return -EFAULT;
			ptr += sizeof(tr);
			binder_transaction(proc, thread, &tr, cmd == BC_REPLY);
			break;
		}

		case BC_REGISTER_LOOPER:
			binder_debug(BINDER_DEBUG_THREADS,
				     "%d:%d BC_REGISTER_LOOPER\n",
				     proc->pid, thread->pid);
			if (thread->looper & BINDER_LOOPER_STATE_ENTERED) {
				thread->looper |= BINDER_LOOPER_STATE_INVALID;
				binder_user_error("%d:%d ERROR: BC_REGISTER_LOOPER called after BC_ENTER_LOOPER\n",
					proc->pid, thread->pid);
			} else if (proc->requested_threads == 0) {
				thread->looper |= BINDER_LOOPER_STATE_INVALID;
				binder_user_error("%d:%d ERROR: BC_REGISTER_LOOPER called without request\n",
					proc->pid, thread->pid);
			} else {
				proc->requested_threads--;
				proc->requested_threads_started++;
			}
			thread->looper |= BINDER_LOOPER_STATE_REGISTERED;
			break;
		case BC_ENTER_LOOPER:
			binder_debug(BINDER_DEBUG_THREADS,
				     "%d:%d BC_ENTER_LOOPER\n",
				     proc->pid, thread->pid);
			if (thread->looper & BINDER_LOOPER_STATE_REGISTERED) {
				thread->looper |= BINDER_LOOPER_STATE_INVALID;
				binder_user_error("%d:%d ERROR: BC_ENTER_LOOPER called after BC_REGISTER_LOOPER\n",
					proc->pid, thread->pid);
			}
			thread->looper |= BINDER_LOOPER_STATE_ENTERED;
			break;
		case BC_EXIT_LOOPER:
			binder_debug(BINDER_DEBUG_THREADS,
				     "%d:%d BC_EXIT_LOOPER\n",
				     proc->pid, thread->pid);
			thread->looper |= BINDER_LOOPER_STATE_EXITED;
			break;

		case BC_REQUEST_DEATH_NOTIFICATION:
		case BC_CLEAR_DEATH_NOTIFICATION: {
			// target: 该node die之后通知我
			// cookie: 我的联系方式
			uint32_t target;
			void __user *cookie;
			
			struct binder_ref *ref;
			struct binder_ref_death *death;

			if (get_user(target, (uint32_t __user *)ptr))
				return -EFAULT;
			ptr += sizeof(uint32_t);
			if (get_user(cookie, (void __user * __user *)ptr))
				return -EFAULT;
			ptr += sizeof(void *);
			ref = binder_get_ref(proc, target);
			if (ref == NULL) {
				binder_user_error("%d:%d %s invalid ref %d\n",
					proc->pid, thread->pid,
					cmd == BC_REQUEST_DEATH_NOTIFICATION ?
					"BC_REQUEST_DEATH_NOTIFICATION" :
					"BC_CLEAR_DEATH_NOTIFICATION",
					target);
				break;
			}

			binder_debug(BINDER_DEBUG_DEATH_NOTIFICATION,
				     "%d:%d %s %p ref %d desc %d s %d w %d for node %d\n",
				     proc->pid, thread->pid,
				     cmd == BC_REQUEST_DEATH_NOTIFICATION ?
				     "BC_REQUEST_DEATH_NOTIFICATION" :
				     "BC_CLEAR_DEATH_NOTIFICATION",
				     cookie, ref->debug_id, ref->desc,
				     ref->strong, ref->weak, ref->node->debug_id);

			if (cmd == BC_REQUEST_DEATH_NOTIFICATION) {
				if (ref->death) {
					binder_user_error("%d:%d BC_REQUEST_DEATH_NOTIFICATION death notification already set\n",
						proc->pid, thread->pid);
					break;
				}
				death = kzalloc(sizeof(*death), GFP_KERNEL);
				if (death == NULL) {
					thread->return_error = BR_ERROR;
					binder_debug(BINDER_DEBUG_FAILED_TRANSACTION,
						     "%d:%d BC_REQUEST_DEATH_NOTIFICATION failed\n",
						     proc->pid, thread->pid);
					break;
				}
				binder_stats_created(BINDER_STAT_DEATH);
				INIT_LIST_HEAD(&death->work.entry);
				death->cookie = cookie;
				ref->death = death;
				if (ref->node->proc == NULL) {
					// 如果目标node所在的进程已经挂了，那么给要求注册的线程/进程添加一个work，
					// 该work的类型就是BINDER_WORK_DEAD_BINDER，即告诉它，目标binder已经dead了。
					ref->death->work.type = BINDER_WORK_DEAD_BINDER;
					// 如果当前进程是pool thread，就把work交给它来处理，否则交给其所在的进程来处理，
					// 这个很好理解，比如client调用linkToDeath()，就会发送request death noti消息，
					// 但是client一般不是pool thread(除非它是另一个service的server端)，那么就把
					// dead binder这个work挂到该client的proc->todo列表上，并唤醒其上的等待队列。
					if (thread->looper & (BINDER_LOOPER_STATE_REGISTERED | BINDER_LOOPER_STATE_ENTERED)) {
						list_add_tail(&ref->death->work.entry, &thread->todo);
					} else {
						list_add_tail(&ref->death->work.entry, &proc->todo);
						wake_up_interruptible(&proc->wait); // 唤醒等待队列，某个线程处理任务时会处理proc->todo上的work，
															 // 见binder_thread_read()函数
					}
				}
			} else {
				if (ref->death == NULL) {
					binder_user_error("%d:%d BC_CLEAR_DEATH_NOTIFICATION death notification not active\n",
						proc->pid, thread->pid);
					break;
				}
				death = ref->death;
				if (death->cookie != cookie) {
					binder_user_error("%d:%d BC_CLEAR_DEATH_NOTIFICATION death notification cookie mismatch %p != %p\n",
						proc->pid, thread->pid,
						death->cookie, cookie);
					break;
				}
				ref->death = NULL;
				if (list_empty(&death->work.entry)) {
					death->work.type = BINDER_WORK_CLEAR_DEATH_NOTIFICATION;
					if (thread->looper & (BINDER_LOOPER_STATE_REGISTERED | BINDER_LOOPER_STATE_ENTERED)) {
						list_add_tail(&death->work.entry, &thread->todo);
					} else {
						list_add_tail(&death->work.entry, &proc->todo);
						wake_up_interruptible(&proc->wait);
					}
				} else {
					BUG_ON(death->work.type != BINDER_WORK_DEAD_BINDER);
					death->work.type = BINDER_WORK_DEAD_BINDER_AND_CLEAR;
				}
			}
		} break;
		case BC_DEAD_BINDER_DONE: {
			struct binder_work *w;
			void __user *cookie;
			struct binder_ref_death *death = NULL;
			if (get_user(cookie, (void __user * __user *)ptr))
				return -EFAULT;

			ptr += sizeof(void *);
			list_for_each_entry(w, &proc->delivered_death, entry) {
				struct binder_ref_death *tmp_death = container_of(w, struct binder_ref_death, work);
				if (tmp_death->cookie == cookie) {
					death = tmp_death;
					break;
				}
			}
			binder_debug(BINDER_DEBUG_DEAD_BINDER,
				     "%d:%d BC_DEAD_BINDER_DONE %p found %p\n",
				     proc->pid, thread->pid, cookie, death);
			if (death == NULL) {
				binder_user_error("%d:%d BC_DEAD_BINDER_DONE %p not found\n",
					proc->pid, thread->pid, cookie);
				break;
			}

			list_del_init(&death->work.entry);
			if (death->work.type == BINDER_WORK_DEAD_BINDER_AND_CLEAR) {
				death->work.type = BINDER_WORK_CLEAR_DEATH_NOTIFICATION;
				if (thread->looper & (BINDER_LOOPER_STATE_REGISTERED | BINDER_LOOPER_STATE_ENTERED)) {
					list_add_tail(&death->work.entry, &thread->todo);
				} else {
					list_add_tail(&death->work.entry, &proc->todo);
					wake_up_interruptible(&proc->wait);
				}
			}
		} break;

		default:
			pr_err("%d:%d unknown command %d\n",
			       proc->pid, thread->pid, cmd);
			return -EINVAL;
		}
		*consumed = ptr - buffer;
	}
	return 0;
}

void binder_stat_br(struct binder_proc *proc, struct binder_thread *thread,
		    uint32_t cmd)
{
	trace_binder_return(cmd);
	if (_IOC_NR(cmd) < ARRAY_SIZE(binder_stats.br)) {
		binder_stats.br[_IOC_NR(cmd)]++;
		proc->stats.br[_IOC_NR(cmd)]++;
		thread->stats.br[_IOC_NR(cmd)]++;
	}
}

static int binder_has_proc_work(struct binder_proc *proc,
				struct binder_thread *thread)
{
	return !list_empty(&proc->todo) ||
		(thread->looper & BINDER_LOOPER_STATE_NEED_RETURN);
}

static int binder_has_thread_work(struct binder_thread *thread)
{
	return !list_empty(&thread->todo) || thread->return_error != BR_OK ||
		(thread->looper & BINDER_LOOPER_STATE_NEED_RETURN);
}

// 把指令发往user space
static int binder_thread_read(struct binder_proc *proc,
			      struct binder_thread *thread,
			      void  __user *buffer, int size,
			      signed long *consumed, int non_block)
{
	void __user *ptr = buffer + *consumed;
	void __user *end = buffer + size;

	int ret = 0;
	int wait_for_proc_work;

	if (*consumed == 0) {
		if (put_user(BR_NOOP, (uint32_t __user *)ptr))
			return -EFAULT;
		ptr += sizeof(uint32_t);
	}

retry:
	wait_for_proc_work = thread->transaction_stack == NULL &&
				list_empty(&thread->todo);

	// 返回write error
	if (thread->return_error != BR_OK && ptr < end) {
		if (thread->return_error2 != BR_OK) {
			if (put_user(thread->return_error2, (uint32_t __user *)ptr))
				return -EFAULT;
			ptr += sizeof(uint32_t);
			binder_stat_br(proc, thread, thread->return_error2);
			if (ptr == end)
				goto done;
			thread->return_error2 = BR_OK;
		}
		if (put_user(thread->return_error, (uint32_t __user *)ptr))
			return -EFAULT;
		ptr += sizeof(uint32_t);
		binder_stat_br(proc, thread, thread->return_error);
		thread->return_error = BR_OK;
		goto done;
	}


	// 判断是否有work需要处理，并根据non-block参数选择进入睡眠状态还是立即返回
	thread->looper |= BINDER_LOOPER_STATE_WAITING;
	if (wait_for_proc_work)
		proc->ready_threads++;

	binder_unlock(__func__);

	trace_binder_wait_for_work(wait_for_proc_work,
				   !!thread->transaction_stack,
				   !list_empty(&thread->todo));
	if (wait_for_proc_work) {
		// thread没啥需要处理的work，看看proc有没有
		if (!(thread->looper & (BINDER_LOOPER_STATE_REGISTERED |
					BINDER_LOOPER_STATE_ENTERED))) {
			binder_user_error("%d:%d ERROR: Thread waiting for process work before calling BC_REGISTER_LOOPER or BC_ENTER_LOOPER (state %x)\n",
				proc->pid, thread->pid, thread->looper);
			wait_event_interruptible(binder_user_error_wait,
						 binder_stop_on_user_error < 2);
		}
		binder_set_nice(proc->default_priority);
		if (non_block) {
			if (!binder_has_proc_work(proc, thread))
				ret = -EAGAIN;
		} else
			// 进入排他性等待状态
			ret = wait_event_interruptible_exclusive(proc->wait, binder_has_proc_work(proc, thread));
	} else {
		// thread有需要处理的work，
		if (non_block) {
			if (!binder_has_thread_work(thread))
				ret = -EAGAIN;
		} else
			// 进入正常等待状态
			ret = wait_event_interruptible(thread->wait, binder_has_thread_work(thread));
	}
	/*
		注: 排他性等待 vs 正常等待，
			CPU在唤醒等待队列时，一次性唤醒所有等待进程，即将其状态设置为runnable，但是如果等待队列里面有多个排他性等待状态
			的进程，则选择其中一个唤醒，该队列中的其他正常等待状态的进程正常唤醒(不受影响)。
	 */

	binder_lock(__func__);

	if (wait_for_proc_work)
		proc->ready_threads--;
	thread->looper &= ~BINDER_LOOPER_STATE_WAITING;

	// 如果需要立即返回(-EAGAIN)或者从睡眠状态醒来时出现异常，直接返回。
	if (ret)
		return ret;

	// 走到这里，说明有任务需要处理啦~~~，从上面的睡眠来看，似乎有两种可能啊，proc work或者thread work? 嗯，下面一次性处理了
	while (1) {
		uint32_t cmd;
		struct binder_transaction_data tr; // 需要return给user space的数据
		struct binder_work *w; // todo list里面需要处理的work
		struct binder_transaction *t = NULL; // 需要当前thread处理的transaction，当前thread正在read...

		// 不管是从proc->wait醒来还是从thread->wait醒来，有work(管他是proc的还是thread的)就要做啦~~~
		if (!list_empty(&thread->todo))
			w = list_first_entry(&thread->todo, struct binder_work, entry);
		else if (!list_empty(&proc->todo) && wait_for_proc_work)
			w = list_first_entry(&proc->todo, struct binder_work, entry);
		else {
			if (ptr - buffer == 4 && !(thread->looper & BINDER_LOOPER_STATE_NEED_RETURN)) /* no data added */
				goto retry;
			break;
		}

		if (end - ptr < sizeof(tr) + 4) // 呃，，，有异常啊!
			break;

		// 拿到work，开工啦~~~，根据不同的work type，分别处理
		switch (w->type) {
		case BINDER_WORK_TRANSACTION: {
			// transaction类型: 取出transaction data
			t = container_of(w, struct binder_transaction, work);
		} break;
		case BINDER_WORK_TRANSACTION_COMPLETE: {
			// transaction done. 只返回一个cmd，没有任何参数奥~
			cmd = BR_TRANSACTION_COMPLETE;
			if (put_user(cmd, (uint32_t __user *)ptr))
				return -EFAULT;
			ptr += sizeof(uint32_t);

			binder_stat_br(proc, thread, cmd);
			binder_debug(BINDER_DEBUG_TRANSACTION_COMPLETE,
				     "%d:%d BR_TRANSACTION_COMPLETE\n",
				     proc->pid, thread->pid);

			// 工作完成，清理内存，好少年啊~~~
			list_del(&w->entry);
			kfree(w);
			binder_stats_deleted(BINDER_STAT_TRANSACTION_COMPLETE);
		} break;
		case BINDER_WORK_NODE: {
			// node相关的操作，即强弱指针计数增减等操作
			struct binder_node *node = container_of(w, struct binder_node, work); // get node data
			uint32_t cmd = BR_NOOP;
			const char *cmd_name;
			int strong = node->internal_strong_refs || node->local_strong_refs;
			int weak = !hlist_empty(&node->refs) || node->local_weak_refs || strong;
			if (weak && !node->has_weak_ref) {
				cmd = BR_INCREFS;
				cmd_name = "BR_INCREFS";
				node->has_weak_ref = 1;
				node->pending_weak_ref = 1;
				node->local_weak_refs++;
			} else if (strong && !node->has_strong_ref) {
				cmd = BR_ACQUIRE;
				cmd_name = "BR_ACQUIRE";
				node->has_strong_ref = 1;
				node->pending_strong_ref = 1;
				node->local_strong_refs++;
			} else if (!strong && node->has_strong_ref) {
				cmd = BR_RELEASE;
				cmd_name = "BR_RELEASE";
				node->has_strong_ref = 0;
			} else if (!weak && node->has_weak_ref) {
				cmd = BR_DECREFS;
				cmd_name = "BR_DECREFS";
				node->has_weak_ref = 0;
			}
			if (cmd != BR_NOOP) {
				// 有指令需要返回
				// 1. cmd
				if (put_user(cmd, (uint32_t __user *)ptr))
					return -EFAULT;
				ptr += sizeof(uint32_t);
				// 2. ptr
				if (put_user(node->ptr, (void * __user *)ptr))
					return -EFAULT;
				ptr += sizeof(void *);
				// 3. cookie
				if (put_user(node->cookie, (void * __user *)ptr))
					return -EFAULT;
				ptr += sizeof(void *);

				binder_stat_br(proc, thread, cmd);
				binder_debug(BINDER_DEBUG_USER_REFS,
					     "%d:%d %s %d u%p c%p\n",
					     proc->pid, thread->pid, cmd_name, node->debug_id, node->ptr, node->cookie);
			} else {
				// 没有啥可做的
				list_del_init(&w->entry); // work entry被移除了，但是并没有清理啊???
				if (!weak && !strong) {
					// 如果stong == weak == 0，那就要清理资源啦
					binder_debug(BINDER_DEBUG_INTERNAL_REFS,
						     "%d:%d node %d u%p c%p deleted\n",
						     proc->pid, thread->pid, node->debug_id,
						     node->ptr, node->cookie);
					// 从这里的清理工作可以看出，node是挂在proc->nodes列表上的
					rb_erase(&node->rb_node, &proc->nodes);
					kfree(node);
					binder_stats_deleted(BINDER_STAT_NODE);
				} else {
					binder_debug(BINDER_DEBUG_INTERNAL_REFS,
						     "%d:%d node %d u%p c%p state unchanged\n",
						     proc->pid, thread->pid, node->debug_id, node->ptr,
						     node->cookie);
				}
			}
		} break;
		case BINDER_WORK_DEAD_BINDER:
		case BINDER_WORK_DEAD_BINDER_AND_CLEAR:
		case BINDER_WORK_CLEAR_DEATH_NOTIFICATION: {
			// death notification相关的work
			
			struct binder_ref_death *death;
			uint32_t cmd;

			death = container_of(w, struct binder_ref_death, work); // get data first
			if (w->type == BINDER_WORK_CLEAR_DEATH_NOTIFICATION)
				cmd = BR_CLEAR_DEATH_NOTIFICATION_DONE;
			else
				cmd = BR_DEAD_BINDER;
			if (put_user(cmd, (uint32_t __user *)ptr))
				return -EFAULT;
			ptr += sizeof(uint32_t);
			if (put_user(death->cookie, (void * __user *)ptr))
				return -EFAULT;
			ptr += sizeof(void *);
			binder_stat_br(proc, thread, cmd);
			binder_debug(BINDER_DEBUG_DEATH_NOTIFICATION,
				     "%d:%d %s %p\n",
				      proc->pid, thread->pid,
				      cmd == BR_DEAD_BINDER ?
				      "BR_DEAD_BINDER" :
				      "BR_CLEAR_DEATH_NOTIFICATION_DONE",
				      death->cookie);

			if (w->type == BINDER_WORK_CLEAR_DEATH_NOTIFICATION) {
				// 相比于binder_node，binder_ref_death则没有一个统一的挂载列表
				list_del(&w->entry);
				kfree(death);
				binder_stats_deleted(BINDER_STAT_DEATH);
			} else
				list_move(&w->entry, &proc->delivered_death); // death类型的work还要挂载到proc->delivered_death列表上
			if (cmd == BR_DEAD_BINDER)
				goto done; /* DEAD_BINDER notifications can cause transactions */
		} break;
		}

		// node相关的work, death相关的work直接操作完了，剩下的就是transaction相关的work，如果有的话
		if (!t)
			continue;

		// 下面是transaction相关的操作，重头戏，重点是构建binder_transaction_data数据结构用以返回给user space
		BUG_ON(t->buffer == NULL);
		// target_node对应到user space的某个BpBinder
		if (t->buffer->target_node) {
			struct binder_node *target_node = t->buffer->target_node;
			tr.target.ptr = target_node->ptr;
			tr.cookie =  target_node->cookie;
			t->saved_priority = task_nice(current);
			if (t->priority < target_node->min_priority &&
			    !(t->flags & TF_ONE_WAY))
				binder_set_nice(t->priority);
			else if (!(t->flags & TF_ONE_WAY) ||
				 t->saved_priority > target_node->min_priority)
				binder_set_nice(target_node->min_priority);
			cmd = BR_TRANSACTION;
		} else {
			tr.target.ptr = NULL;
			tr.cookie = NULL;
			cmd = BR_REPLY;
		}
		tr.code = t->code;
		tr.flags = t->flags;
		tr.sender_euid = from_kuid(current_user_ns(), t->sender_euid);

		if (t->from) {
			struct task_struct *sender = t->from->proc->tsk;
			tr.sender_pid = task_tgid_nr_ns(sender,
							task_active_pid_ns(current));
		} else {
			tr.sender_pid = 0;
		}

		tr.data_size = t->buffer->data_size;
		tr.offsets_size = t->buffer->offsets_size;
		tr.data.ptr.buffer = (void *)t->buffer->data + proc->user_buffer_offset;
		tr.data.ptr.offsets = tr.data.ptr.buffer + ALIGN(t->buffer->data_size, sizeof(void *));

		// 写回cmd
		if (put_user(cmd, (uint32_t __user *)ptr))
			return -EFAULT;
		ptr += sizeof(uint32_t);
		// 写回binder_transaction_data
		if (copy_to_user(ptr, &tr, sizeof(tr)))
			return -EFAULT;
		ptr += sizeof(tr);

		trace_binder_transaction_received(t);
		binder_stat_br(proc, thread, cmd);
		binder_debug(BINDER_DEBUG_TRANSACTION,
			     "%d:%d %s %d %d:%d, cmd %d size %zd-%zd ptr %p-%p\n",
			     proc->pid, thread->pid,
			     (cmd == BR_TRANSACTION) ? "BR_TRANSACTION" :
			     "BR_REPLY",
			     t->debug_id, t->from ? t->from->proc->pid : 0,
			     t->from ? t->from->pid : 0, cmd,
			     t->buffer->data_size, t->buffer->offsets_size,
			     tr.data.ptr.buffer, tr.data.ptr.offsets);

		list_del(&t->work.entry);
		t->buffer->allow_user_free = 1;
		if (cmd == BR_TRANSACTION && !(t->flags & TF_ONE_WAY)) { // 从todo list接收来的transaction，挂载到txn stack上了，因为reply时会用到
																 // 见binder_transaction()里面reply相关的部分(in_reply_to)。
			t->to_parent = thread->transaction_stack;
			t->to_thread = thread;
			thread->transaction_stack = t;
		} else {
			t->buffer->transaction = NULL;
			kfree(t);
			binder_stats_deleted(BINDER_STAT_TRANSACTION);
		}
		break;
	}

done:

	*consumed = ptr - buffer;
	if (proc->requested_threads + proc->ready_threads == 0 &&
	    proc->requested_threads_started < proc->max_threads &&
	    (thread->looper & (BINDER_LOOPER_STATE_REGISTERED |
	     BINDER_LOOPER_STATE_ENTERED)) /* the user-space code fails to */
	     /*spawn a new thread if we leave this out */) {
		proc->requested_threads++;
		binder_debug(BINDER_DEBUG_THREADS,
			     "%d:%d BR_SPAWN_LOOPER\n",
			     proc->pid, thread->pid);
		if (put_user(BR_SPAWN_LOOPER, (uint32_t __user *)buffer)) // 要求user-space增开loop thread
			return -EFAULT;
		binder_stat_br(proc, thread, BR_SPAWN_LOOPER);
	}
	return 0;
}

static void binder_release_work(struct list_head *list)
{
	struct binder_work *w;
	while (!list_empty(list)) {
		w = list_first_entry(list, struct binder_work, entry);
		list_del_init(&w->entry);
		switch (w->type) {
		case BINDER_WORK_TRANSACTION: {
			struct binder_transaction *t;

			t = container_of(w, struct binder_transaction, work);
			if (t->buffer->target_node &&
			    !(t->flags & TF_ONE_WAY)) {
				binder_send_failed_reply(t, BR_DEAD_REPLY);
			} else {
				binder_debug(BINDER_DEBUG_DEAD_TRANSACTION,
					"undelivered transaction %d\n",
					t->debug_id);
				t->buffer->transaction = NULL;
				kfree(t);
				binder_stats_deleted(BINDER_STAT_TRANSACTION);
			}
		} break;
		case BINDER_WORK_TRANSACTION_COMPLETE: {
			binder_debug(BINDER_DEBUG_DEAD_TRANSACTION,
				"undelivered TRANSACTION_COMPLETE\n");
			kfree(w);
			binder_stats_deleted(BINDER_STAT_TRANSACTION_COMPLETE);
		} break;
		case BINDER_WORK_DEAD_BINDER_AND_CLEAR:
		case BINDER_WORK_CLEAR_DEATH_NOTIFICATION: {
			struct binder_ref_death *death;

			death = container_of(w, struct binder_ref_death, work);
			binder_debug(BINDER_DEBUG_DEAD_TRANSACTION,
				"undelivered death notification, %p\n",
				death->cookie);
			kfree(death);
			binder_stats_deleted(BINDER_STAT_DEATH);
		} break;
		default:
			pr_err("unexpected work type, %d, not freed\n",
			       w->type);
			break;
		}
	}

}

// 从struct binder_proc的threads红黑树中，根据pid寻找binder_thread对象，
// 如果找不到就创建一个新的。
static struct binder_thread *binder_get_thread(struct binder_proc *proc)
{
	struct binder_thread *thread = NULL;
	struct rb_node *parent = NULL;
	struct rb_node **p = &proc->threads.rb_node;

	while (*p) {
		parent = *p;
		thread = rb_entry(parent, struct binder_thread, rb_node);

		if (current->pid < thread->pid)
			p = &(*p)->rb_left;
		else if (current->pid > thread->pid)
			p = &(*p)->rb_right;
		else
			break;
	}
	// 找不到就创建一个新的
	if (*p == NULL) {
		thread = kzalloc(sizeof(*thread), GFP_KERNEL);
		if (thread == NULL)
			return NULL;
		binder_stats_created(BINDER_STAT_THREAD);
		thread->proc = proc;
		thread->pid = current->pid; // 这里的pid其实是线程id，即tid
		init_waitqueue_head(&thread->wait);
		INIT_LIST_HEAD(&thread->todo);
		rb_link_node(&thread->rb_node, parent, p);
		rb_insert_color(&thread->rb_node, &proc->threads);
		thread->looper |= BINDER_LOOPER_STATE_NEED_RETURN;
		thread->return_error = BR_OK;
		thread->return_error2 = BR_OK;
	}
	return thread;
}

static int binder_free_thread(struct binder_proc *proc,
			      struct binder_thread *thread)
{
	struct binder_transaction *t;
	struct binder_transaction *send_reply = NULL;
	int active_transactions = 0;

	rb_erase(&thread->rb_node, &proc->threads);
	t = thread->transaction_stack;
	if (t && t->to_thread == thread)
		send_reply = t;
	while (t) {
		active_transactions++;
		binder_debug(BINDER_DEBUG_DEAD_TRANSACTION,
			     "release %d:%d transaction %d %s, still active\n",
			      proc->pid, thread->pid,
			     t->debug_id,
			     (t->to_thread == thread) ? "in" : "out");

		if (t->to_thread == thread) {
			t->to_proc = NULL;
			t->to_thread = NULL;
			if (t->buffer) {
				t->buffer->transaction = NULL;
				t->buffer = NULL; // buffer什么时候清理?
			}
			t = t->to_parent;
		} else if (t->from == thread) {
			t->from = NULL;
			t = t->from_parent;
		} else
			BUG();
	}
	if (send_reply)
		binder_send_failed_reply(send_reply, BR_DEAD_REPLY);
	binder_release_work(&thread->todo);
	kfree(thread); // 释放内存
	binder_stats_deleted(BINDER_STAT_THREAD);
	return active_transactions;
}

/* file operation function */
static unsigned int binder_poll(struct file *filp,
				struct poll_table_struct *wait)
{
	struct binder_proc *proc = filp->private_data;
	struct binder_thread *thread = NULL;
	int wait_for_proc_work;

	binder_lock(__func__);

	thread = binder_get_thread(proc);

	wait_for_proc_work = thread->transaction_stack == NULL &&
		list_empty(&thread->todo) && thread->return_error == BR_OK;

	binder_unlock(__func__);

	if (wait_for_proc_work) {
		if (binder_has_proc_work(proc, thread))
			return POLLIN;
		poll_wait(filp, &proc->wait, wait);
		if (binder_has_proc_work(proc, thread))
			return POLLIN;
	} else {
		if (binder_has_thread_work(thread))
			return POLLIN;
		poll_wait(filp, &thread->wait, wait);
		if (binder_has_thread_work(thread))
			return POLLIN;
	}
	return 0;
}

/* file operation function */
static long binder_ioctl(struct file *filp, unsigned int cmd, unsigned long arg)
{
	int ret;
	struct binder_proc *proc = filp->private_data;
	struct binder_thread *thread;
	// 参数arg的size，注意cmd(IOWR, IOW etc)在定义时，最后一个字段提供的是参数类型，
	// 在这里可以得出它的size
	unsigned int size = _IOC_SIZE(cmd);
	void __user *ubuf = (void __user *)arg;

	/*pr_info("binder_ioctl: %d:%d %x %lx\n", proc->pid, current->pid, cmd, arg);*/

	trace_binder_ioctl(cmd, arg);

	ret = wait_event_interruptible(binder_user_error_wait, binder_stop_on_user_error < 2);
	if (ret)
		goto err_unlocked;

	binder_lock(__func__);
	// 查找 or 创建 thread对象
	thread = binder_get_thread(proc);
	if (thread == NULL) {
		ret = -ENOMEM;
		goto err;
	}

	// ioctl指令都在这里处理，最关键的就是BINDER_WRITE_READ指令
	switch (cmd) {
	case BINDER_WRITE_READ: {
		struct binder_write_read bwr;
		if (size != sizeof(struct binder_write_read)) {
			ret = -EINVAL;
			goto err;
		}
		if (copy_from_user(&bwr, ubuf, sizeof(bwr))) {
			ret = -EFAULT;
			goto err;
		}
		binder_debug(BINDER_DEBUG_READ_WRITE,
			     "%d:%d write %ld at %08lx, read %ld at %08lx\n",
			     proc->pid, thread->pid, bwr.write_size,
			     bwr.write_buffer, bwr.read_size, bwr.read_buffer);

		if (bwr.write_size > 0) {
			ret = binder_thread_write(proc, thread, (void __user *)bwr.write_buffer, bwr.write_size, &bwr.write_consumed);
			trace_binder_write_done(ret);
			if (ret < 0) {
				bwr.read_consumed = 0;
				if (copy_to_user(ubuf, &bwr, sizeof(bwr)))
					ret = -EFAULT;
				goto err;
			}
		}
		if (bwr.read_size > 0) {
			ret = binder_thread_read(proc, thread, (void __user *)bwr.read_buffer, bwr.read_size, &bwr.read_consumed, filp->f_flags & O_NONBLOCK);
			trace_binder_read_done(ret);
			if (!list_empty(&proc->todo))
				wake_up_interruptible(&proc->wait); // 如果proc->todo非空，那就唤醒其等待队列(来do work)
			if (ret < 0) {
				if (copy_to_user(ubuf, &bwr, sizeof(bwr)))
					ret = -EFAULT;
				goto err;
			}
		}
		binder_debug(BINDER_DEBUG_READ_WRITE,
			     "%d:%d wrote %ld of %ld, read return %ld of %ld\n",
			     proc->pid, thread->pid, bwr.write_consumed, bwr.write_size,
			     bwr.read_consumed, bwr.read_size);
		// put_user()只是完成数据填充，copy_to_user()才是真正的弄到user space里面去
		if (copy_to_user(ubuf, &bwr, sizeof(bwr))) {
			ret = -EFAULT;
			goto err;
		}
		break;
	}
	case BINDER_SET_MAX_THREADS:
		if (copy_from_user(&proc->max_threads, ubuf, sizeof(proc->max_threads))) {
			ret = -EINVAL;
			goto err;
		}
		break;
	case BINDER_SET_CONTEXT_MGR:
		if (binder_context_mgr_node != NULL) {
			pr_err("BINDER_SET_CONTEXT_MGR already set\n");
			ret = -EBUSY;
			goto err;
		}
		if (uid_valid(binder_context_mgr_uid)) {
			// 如果uid对应不起来，那么就是非法操作，即只有第一个调用该指令的进程才是合法的manager
			if (!uid_eq(binder_context_mgr_uid, current->cred->euid)) {
				pr_err("BINDER_SET_CONTEXT_MGR bad uid %d != %d\n",
				       from_kuid(&init_user_ns, current->cred->euid),
				       from_kuid(&init_user_ns, binder_context_mgr_uid));
				ret = -EPERM;
				goto err;
			}
		} else // 初始化时，binder_context_mgr_uid = INVALID_UID;所以，第一个调用该指令的进程将成为manager
			binder_context_mgr_uid = current->cred->euid;

		// 此时运行的进程即为servier manager所在的进程，否则就goto err了
		binder_context_mgr_node = binder_new_node(proc, NULL, NULL); // context mgr node的ptr, cookie均为 0
		if (binder_context_mgr_node == NULL) {
			ret = -ENOMEM;
			goto err;
		}
		binder_context_mgr_node->local_weak_refs++;
		binder_context_mgr_node->local_strong_refs++;
		binder_context_mgr_node->has_strong_ref = 1;
		binder_context_mgr_node->has_weak_ref = 1;
		break;
	case BINDER_THREAD_EXIT:
		binder_debug(BINDER_DEBUG_THREADS, "%d:%d exit\n",
			     proc->pid, thread->pid);
		// 释放内存
		binder_free_thread(proc, thread);
		thread = NULL;
		break;
	case BINDER_VERSION:
		if (size != sizeof(struct binder_version)) {
			ret = -EINVAL;
			goto err;
		}
		if (put_user(BINDER_CURRENT_PROTOCOL_VERSION, &((struct binder_version *)ubuf)->protocol_version)) {
			ret = -EINVAL;
			goto err;
		}
		break;
	default:
		ret = -EINVAL;
		goto err;
	}
	ret = 0;
err:
	if (thread)
		thread->looper &= ~BINDER_LOOPER_STATE_NEED_RETURN;
	binder_unlock(__func__);
	wait_event_interruptible(binder_user_error_wait, binder_stop_on_user_error < 2);
	if (ret && ret != -ERESTARTSYS)
		pr_info("%d:%d ioctl %x %lx returned %d\n", proc->pid, current->pid, cmd, arg, ret);
err_unlocked:
	trace_binder_ioctl_done(ret);
	return ret;
}

static void binder_vma_open(struct vm_area_struct *vma)
{
	struct binder_proc *proc = vma->vm_private_data;
	binder_debug(BINDER_DEBUG_OPEN_CLOSE,
		     "%d open vm area %lx-%lx (%ld K) vma %lx pagep %lx\n",
		     proc->pid, vma->vm_start, vma->vm_end,
		     (vma->vm_end - vma->vm_start) / SZ_1K, vma->vm_flags,
		     (unsigned long)pgprot_val(vma->vm_page_prot));
}

static void binder_vma_close(struct vm_area_struct *vma)
{
	struct binder_proc *proc = vma->vm_private_data;
	binder_debug(BINDER_DEBUG_OPEN_CLOSE,
		     "%d close vm area %lx-%lx (%ld K) vma %lx pagep %lx\n",
		     proc->pid, vma->vm_start, vma->vm_end,
		     (vma->vm_end - vma->vm_start) / SZ_1K, vma->vm_flags,
		     (unsigned long)pgprot_val(vma->vm_page_prot));
	proc->vma = NULL;
	proc->vma_vm_mm = NULL;
	binder_defer_work(proc, BINDER_DEFERRED_PUT_FILES);
}

static struct vm_operations_struct binder_vm_ops = {
	.open = binder_vma_open,
	.close = binder_vma_close,
};

/* file operation function */
static int binder_mmap(struct file *filp, struct vm_area_struct *vma)
{
	int ret;
	struct vm_struct *area;
	struct binder_proc *proc = filp->private_data;
	const char *failure_string;
	struct binder_buffer *buffer;

	if (proc->tsk != current)
		return -EINVAL;

	if ((vma->vm_end - vma->vm_start) > SZ_4M)
		vma->vm_end = vma->vm_start + SZ_4M;

	binder_debug(BINDER_DEBUG_OPEN_CLOSE,
		     "binder_mmap: %d %lx-%lx (%ld K) vma %lx pagep %lx\n",
		     proc->pid, vma->vm_start, vma->vm_end,
		     (vma->vm_end - vma->vm_start) / SZ_1K, vma->vm_flags,
		     (unsigned long)pgprot_val(vma->vm_page_prot));

	if (vma->vm_flags & FORBIDDEN_MMAP_FLAGS) {
		ret = -EPERM;
		failure_string = "bad vm_flags";
		goto err_bad_arg;
	}
	vma->vm_flags = (vma->vm_flags | VM_DONTCOPY) & ~VM_MAYWRITE;

	mutex_lock(&binder_mmap_lock);
	if (proc->buffer) {
		ret = -EBUSY;
		failure_string = "already mapped";
		goto err_already_mapped;
	}

	// 分配虚拟内存，此为一段连续的内核虚拟内存
	area = get_vm_area(vma->vm_end - vma->vm_start, VM_IOREMAP);
	if (area == NULL) {
		ret = -ENOMEM;
		failure_string = "get_vm_area";
		goto err_get_vm_area_failed;
	}
	proc->buffer = area->addr; // buffer执行虚拟内存的开始位置
	proc->user_buffer_offset = vma->vm_start - (uintptr_t)proc->buffer;
	mutex_unlock(&binder_mmap_lock);

#ifdef CONFIG_CPU_CACHE_VIPT
	if (cache_is_vipt_aliasing()) {
		while (CACHE_COLOUR((vma->vm_start ^ (uint32_t)proc->buffer))) {
			pr_info("binder_mmap: %d %lx-%lx maps %p bad alignment\n", proc->pid, vma->vm_start, vma->vm_end, proc->buffer);
			vma->vm_start += PAGE_SIZE;
		}
	}
#endif
	// 为pages指针数组分配内存，这只是分配了指针数组的内存，该数组的每一个元素都是执向page的指针，
	// 而struct page的实际内存还没有分配
	// proc->pages[0]是一个struct page*，所以其sizeof就等于sizeof(void*)，一个指针的大小。
	proc->pages = kzalloc(sizeof(proc->pages[0]) * ((vma->vm_end - vma->vm_start) / PAGE_SIZE), GFP_KERNEL);
	if (proc->pages == NULL) {
		ret = -ENOMEM;
		failure_string = "alloc page array";
		goto err_alloc_pages_failed;
	}
	proc->buffer_size = vma->vm_end - vma->vm_start;

	vma->vm_ops = &binder_vm_ops;
	vma->vm_private_data = proc;

	if (binder_update_page_range(proc, 1, proc->buffer, proc->buffer + PAGE_SIZE, vma)) {
		ret = -ENOMEM;
		failure_string = "alloc small buf";
		goto err_alloc_small_buf_failed;
	}
	buffer = proc->buffer;
	INIT_LIST_HEAD(&proc->buffers);
	list_add(&buffer->entry, &proc->buffers); // 加入buffers列表
	buffer->free = 1;
	binder_insert_free_buffer(proc, buffer);
	proc->free_async_space = proc->buffer_size / 2;
	barrier();
	proc->files = get_files_struct(current);
	proc->vma = vma;
	proc->vma_vm_mm = vma->vm_mm;

	/*pr_info("binder_mmap: %d %lx-%lx maps %p\n",
		 proc->pid, vma->vm_start, vma->vm_end, proc->buffer);*/
	return 0;

err_alloc_small_buf_failed:
	kfree(proc->pages);
	proc->pages = NULL;
err_alloc_pages_failed:
	mutex_lock(&binder_mmap_lock);
	vfree(proc->buffer);
	proc->buffer = NULL;
err_get_vm_area_failed:
err_already_mapped:
	mutex_unlock(&binder_mmap_lock);
err_bad_arg:
	pr_err("binder_mmap: %d %lx-%lx %s failed %d\n",
	       proc->pid, vma->vm_start, vma->vm_end, failure_string, ret);
	return ret;
}

/* file operation function */
static int binder_open(struct inode *nodp, struct file *filp)
{
	struct binder_proc *proc;

	binder_debug(BINDER_DEBUG_OPEN_CLOSE, "binder_open: %d:%d\n",
		     current->group_leader->pid, current->pid);

	proc = kzalloc(sizeof(*proc), GFP_KERNEL);
	if (proc == NULL)
		return -ENOMEM;
	get_task_struct(current);
	proc->tsk = current;
	INIT_LIST_HEAD(&proc->todo);
	init_waitqueue_head(&proc->wait);
	proc->default_priority = task_nice(current);

	binder_lock(__func__);

	binder_stats_created(BINDER_STAT_PROC);
	hlist_add_head(&proc->proc_node, &binder_procs);
	proc->pid = current->group_leader->pid; // 进程的pid用的是group_leader->pid，而不是current->pid
	INIT_LIST_HEAD(&proc->delivered_death);
	filp->private_data = proc;

	binder_unlock(__func__);

	if (binder_debugfs_dir_entry_proc) {
		char strbuf[11];
		snprintf(strbuf, sizeof(strbuf), "%u", proc->pid);
		proc->debugfs_entry = debugfs_create_file(strbuf, S_IRUGO,
			binder_debugfs_dir_entry_proc, proc, &binder_proc_fops);
	}

	return 0;
}

/* file operation function */
static int binder_flush(struct file *filp, fl_owner_t id)
{
	struct binder_proc *proc = filp->private_data;

	binder_defer_work(proc, BINDER_DEFERRED_FLUSH);

	return 0;
}

static void binder_deferred_flush(struct binder_proc *proc)
{
	struct rb_node *n;
	int wake_count = 0;
	for (n = rb_first(&proc->threads); n != NULL; n = rb_next(n)) {
		struct binder_thread *thread = rb_entry(n, struct binder_thread, rb_node);
		thread->looper |= BINDER_LOOPER_STATE_NEED_RETURN;
		if (thread->looper & BINDER_LOOPER_STATE_WAITING) {
			wake_up_interruptible(&thread->wait);
			wake_count++;
		}
	}
	wake_up_interruptible_all(&proc->wait);

	binder_debug(BINDER_DEBUG_OPEN_CLOSE,
		     "binder_flush: %d woke %d threads\n", proc->pid,
		     wake_count);
}

/* file operation function */
static int binder_release(struct inode *nodp, struct file *filp)
{
	struct binder_proc *proc = filp->private_data;
	debugfs_remove(proc->debugfs_entry);
	binder_defer_work(proc, BINDER_DEFERRED_RELEASE);

	return 0;
}

static void binder_deferred_release(struct binder_proc *proc)
{
	struct hlist_node *pos;
	struct binder_transaction *t;
	struct rb_node *n;
	int threads, nodes, incoming_refs, outgoing_refs, buffers, active_transactions, page_count;

	BUG_ON(proc->vma);
	BUG_ON(proc->files);

	hlist_del(&proc->proc_node);
	if (binder_context_mgr_node && binder_context_mgr_node->proc == proc) {
		binder_debug(BINDER_DEBUG_DEAD_BINDER,
			     "binder_release: %d context_mgr_node gone\n",
			     proc->pid);
		binder_context_mgr_node = NULL;
	}

	threads = 0;
	active_transactions = 0;
	while ((n = rb_first(&proc->threads))) {
		struct binder_thread *thread = rb_entry(n, struct binder_thread, rb_node);
		threads++;
		active_transactions += binder_free_thread(proc, thread);
	}
	nodes = 0;
	incoming_refs = 0;
	while ((n = rb_first(&proc->nodes))) {
		struct binder_node *node = rb_entry(n, struct binder_node, rb_node);

		nodes++;
		rb_erase(&node->rb_node, &proc->nodes);
		list_del_init(&node->work.entry);
		binder_release_work(&node->async_todo);
		if (hlist_empty(&node->refs)) {
			kfree(node);
			binder_stats_deleted(BINDER_STAT_NODE);
		} else {
			struct binder_ref *ref;
			int death = 0;

			node->proc = NULL;
			node->local_strong_refs = 0;
			node->local_weak_refs = 0;
			hlist_add_head(&node->dead_node, &binder_dead_nodes);

			hlist_for_each_entry(ref, pos, &node->refs, node_entry) {
				incoming_refs++;
				if (ref->death) {
					death++;
					if (list_empty(&ref->death->work.entry)) {
						ref->death->work.type = BINDER_WORK_DEAD_BINDER;
						list_add_tail(&ref->death->work.entry, &ref->proc->todo);
						wake_up_interruptible(&ref->proc->wait);
					} else
						BUG();
				}
			}
			binder_debug(BINDER_DEBUG_DEAD_BINDER,
				     "node %d now dead, refs %d, death %d\n",
				      node->debug_id, incoming_refs, death);
		}
	}
	outgoing_refs = 0;
	while ((n = rb_first(&proc->refs_by_desc))) {
		struct binder_ref *ref = rb_entry(n, struct binder_ref,
						  rb_node_desc);
		outgoing_refs++;
		binder_delete_ref(ref);
	}
	binder_release_work(&proc->todo);
	binder_release_work(&proc->delivered_death);
	buffers = 0;

	while ((n = rb_first(&proc->allocated_buffers))) {
		struct binder_buffer *buffer = rb_entry(n, struct binder_buffer,
							rb_node);
		t = buffer->transaction;
		if (t) {
			t->buffer = NULL;
			buffer->transaction = NULL;
			pr_err("release proc %d, transaction %d, not freed\n",
			       proc->pid, t->debug_id);
			/*BUG();*/
		}
		binder_free_buf(proc, buffer);
		buffers++;
	}

	binder_stats_deleted(BINDER_STAT_PROC);

	page_count = 0;
	if (proc->pages) {
		int i;
		for (i = 0; i < proc->buffer_size / PAGE_SIZE; i++) {
			if (proc->pages[i]) {
				void *page_addr = proc->buffer + i * PAGE_SIZE;
				binder_debug(BINDER_DEBUG_BUFFER_ALLOC,
					     "binder_release: %d: page %d at %p not freed\n",
					     proc->pid, i,
					     page_addr);
				unmap_kernel_range((unsigned long)page_addr,
					PAGE_SIZE);
				__free_page(proc->pages[i]);
				page_count++;
			}
		}
		kfree(proc->pages);
		vfree(proc->buffer);
	}

	put_task_struct(proc->tsk);

	binder_debug(BINDER_DEBUG_OPEN_CLOSE,
		     "binder_release: %d threads %d, nodes %d (ref %d), refs %d, active transactions %d, buffers %d, pages %d\n",
		     proc->pid, threads, nodes, incoming_refs, outgoing_refs,
		     active_transactions, buffers, page_count);

	kfree(proc);
}

static void binder_deferred_func(struct work_struct *work)
{
	struct binder_proc *proc;
	struct files_struct *files;

	int defer;
	do {
		binder_lock(__func__);
		mutex_lock(&binder_deferred_lock);
		if (!hlist_empty(&binder_deferred_list)) {
			proc = hlist_entry(binder_deferred_list.first,
					struct binder_proc, deferred_work_node);
			hlist_del_init(&proc->deferred_work_node);
			defer = proc->deferred_work;
			proc->deferred_work = 0;
		} else {
			proc = NULL;
			defer = 0;
		}
		mutex_unlock(&binder_deferred_lock);

		files = NULL;
		if (defer & BINDER_DEFERRED_PUT_FILES) {
			files = proc->files;
			if (files)
				proc->files = NULL;
		}

		if (defer & BINDER_DEFERRED_FLUSH)
			binder_deferred_flush(proc);

		if (defer & BINDER_DEFERRED_RELEASE)
			binder_deferred_release(proc); /* frees proc */

		binder_unlock(__func__);
		if (files)
			put_files_struct(files);
	} while (proc);
}
static DECLARE_WORK(binder_deferred_work, binder_deferred_func);

static void
binder_defer_work(struct binder_proc *proc, enum binder_deferred_state defer)
{
	mutex_lock(&binder_deferred_lock);
	proc->deferred_work |= defer;
	if (hlist_unhashed(&proc->deferred_work_node)) {
		hlist_add_head(&proc->deferred_work_node,
				&binder_deferred_list);
		queue_work(binder_deferred_workqueue, &binder_deferred_work);
	}
	mutex_unlock(&binder_deferred_lock);
}

/***************************************************/
/************** 调试函数，可以略过 *****************/
/***************************************************/

static void print_binder_transaction(struct seq_file *m, const char *prefix,
				     struct binder_transaction *t)
{
	seq_printf(m,
		   "%s %d: %p from %d:%d to %d:%d code %x flags %x pri %ld r%d",
		   prefix, t->debug_id, t,
		   t->from ? t->from->proc->pid : 0,
		   t->from ? t->from->pid : 0,
		   t->to_proc ? t->to_proc->pid : 0,
		   t->to_thread ? t->to_thread->pid : 0,
		   t->code, t->flags, t->priority, t->need_reply);
	if (t->buffer == NULL) {
		seq_puts(m, " buffer free\n");
		return;
	}
	if (t->buffer->target_node)
		seq_printf(m, " node %d",
			   t->buffer->target_node->debug_id);
	seq_printf(m, " size %zd:%zd data %p\n",
		   t->buffer->data_size, t->buffer->offsets_size,
		   t->buffer->data);
}

static void print_binder_buffer(struct seq_file *m, const char *prefix,
				struct binder_buffer *buffer)
{
	seq_printf(m, "%s %d: %p size %zd:%zd %s\n",
		   prefix, buffer->debug_id, buffer->data,
		   buffer->data_size, buffer->offsets_size,
		   buffer->transaction ? "active" : "delivered");
}

static void print_binder_work(struct seq_file *m, const char *prefix,
			      const char *transaction_prefix,
			      struct binder_work *w)
{
	struct binder_node *node;
	struct binder_transaction *t;

	switch (w->type) {
	case BINDER_WORK_TRANSACTION:
		t = container_of(w, struct binder_transaction, work);
		print_binder_transaction(m, transaction_prefix, t);
		break;
	case BINDER_WORK_TRANSACTION_COMPLETE:
		seq_printf(m, "%stransaction complete\n", prefix);
		break;
	case BINDER_WORK_NODE:
		node = container_of(w, struct binder_node, work);
		seq_printf(m, "%snode work %d: u%p c%p\n",
			   prefix, node->debug_id, node->ptr, node->cookie);
		break;
	case BINDER_WORK_DEAD_BINDER:
		seq_printf(m, "%shas dead binder\n", prefix);
		break;
	case BINDER_WORK_DEAD_BINDER_AND_CLEAR:
		seq_printf(m, "%shas cleared dead binder\n", prefix);
		break;
	case BINDER_WORK_CLEAR_DEATH_NOTIFICATION:
		seq_printf(m, "%shas cleared death notification\n", prefix);
		break;
	default:
		seq_printf(m, "%sunknown work: type %d\n", prefix, w->type);
		break;
	}
}

static void print_binder_thread(struct seq_file *m,
				struct binder_thread *thread,
				int print_always)
{
	struct binder_transaction *t;
	struct binder_work *w;
	size_t start_pos = m->count;
	size_t header_pos;

	seq_printf(m, "  thread %d: l %02x\n", thread->pid, thread->looper);
	header_pos = m->count;
	t = thread->transaction_stack;
	while (t) {
		if (t->from == thread) {
			print_binder_transaction(m,
						 "    outgoing transaction", t);
			t = t->from_parent;
		} else if (t->to_thread == thread) {
			print_binder_transaction(m,
						 "    incoming transaction", t);
			t = t->to_parent;
		} else {
			print_binder_transaction(m, "    bad transaction", t);
			t = NULL;
		}
	}
	list_for_each_entry(w, &thread->todo, entry) {
		print_binder_work(m, "    ", "    pending transaction", w);
	}
	if (!print_always && m->count == header_pos)
		m->count = start_pos;
}

static void print_binder_node(struct seq_file *m, struct binder_node *node)
{
	struct binder_ref *ref;
	struct hlist_node *pos;
	struct binder_work *w;
	int count;

	count = 0;
	hlist_for_each_entry(ref, pos, &node->refs, node_entry)
		count++;

	seq_printf(m, "  node %d: u%p c%p hs %d hw %d ls %d lw %d is %d iw %d",
		   node->debug_id, node->ptr, node->cookie,
		   node->has_strong_ref, node->has_weak_ref,
		   node->local_strong_refs, node->local_weak_refs,
		   node->internal_strong_refs, count);
	if (count) {
		seq_puts(m, " proc");
		hlist_for_each_entry(ref, pos, &node->refs, node_entry)
			seq_printf(m, " %d", ref->proc->pid);
	}
	seq_puts(m, "\n");
	list_for_each_entry(w, &node->async_todo, entry)
		print_binder_work(m, "    ",
				  "    pending async transaction", w);
}

static void print_binder_ref(struct seq_file *m, struct binder_ref *ref)
{
	seq_printf(m, "  ref %d: desc %d %snode %d s %d w %d d %p\n",
		   ref->debug_id, ref->desc, ref->node->proc ? "" : "dead ",
		   ref->node->debug_id, ref->strong, ref->weak, ref->death);
}

static void print_binder_proc(struct seq_file *m,
			      struct binder_proc *proc, int print_all)
{
	struct binder_work *w;
	struct rb_node *n;
	size_t start_pos = m->count;
	size_t header_pos;

	seq_printf(m, "proc %d\n", proc->pid);
	header_pos = m->count;

	for (n = rb_first(&proc->threads); n != NULL; n = rb_next(n))
		print_binder_thread(m, rb_entry(n, struct binder_thread,
						rb_node), print_all);
	for (n = rb_first(&proc->nodes); n != NULL; n = rb_next(n)) {
		struct binder_node *node = rb_entry(n, struct binder_node,
						    rb_node);
		if (print_all || node->has_async_transaction)
			print_binder_node(m, node);
	}
	if (print_all) {
		for (n = rb_first(&proc->refs_by_desc);
		     n != NULL;
		     n = rb_next(n))
			print_binder_ref(m, rb_entry(n, struct binder_ref,
						     rb_node_desc));
	}
	for (n = rb_first(&proc->allocated_buffers); n != NULL; n = rb_next(n))
		print_binder_buffer(m, "  buffer",
				    rb_entry(n, struct binder_buffer, rb_node));
	list_for_each_entry(w, &proc->todo, entry)
		print_binder_work(m, "  ", "  pending transaction", w);
	list_for_each_entry(w, &proc->delivered_death, entry) {
		seq_puts(m, "  has delivered dead binder\n");
		break;
	}
	if (!print_all && m->count == header_pos)
		m->count = start_pos;
}

static const char *binder_return_strings[] = {
	"BR_ERROR",
	"BR_OK",
	"BR_TRANSACTION",
	"BR_REPLY",
	"BR_ACQUIRE_RESULT",
	"BR_DEAD_REPLY",
	"BR_TRANSACTION_COMPLETE",
	"BR_INCREFS",
	"BR_ACQUIRE",
	"BR_RELEASE",
	"BR_DECREFS",
	"BR_ATTEMPT_ACQUIRE",
	"BR_NOOP",
	"BR_SPAWN_LOOPER",
	"BR_FINISHED",
	"BR_DEAD_BINDER",
	"BR_CLEAR_DEATH_NOTIFICATION_DONE",
	"BR_FAILED_REPLY"
};

static const char *binder_command_strings[] = {
	"BC_TRANSACTION",
	"BC_REPLY",
	"BC_ACQUIRE_RESULT",
	"BC_FREE_BUFFER",
	"BC_INCREFS",
	"BC_ACQUIRE",
	"BC_RELEASE",
	"BC_DECREFS",
	"BC_INCREFS_DONE",
	"BC_ACQUIRE_DONE",
	"BC_ATTEMPT_ACQUIRE",
	"BC_REGISTER_LOOPER",
	"BC_ENTER_LOOPER",
	"BC_EXIT_LOOPER",
	"BC_REQUEST_DEATH_NOTIFICATION",
	"BC_CLEAR_DEATH_NOTIFICATION",
	"BC_DEAD_BINDER_DONE"
};

static const char *binder_objstat_strings[] = {
	"proc",
	"thread",
	"node",
	"ref",
	"death",
	"transaction",
	"transaction_complete"
};

static void print_binder_stats(struct seq_file *m, const char *prefix,
			       struct binder_stats *stats)
{
	int i;

	BUILD_BUG_ON(ARRAY_SIZE(stats->bc) !=
		     ARRAY_SIZE(binder_command_strings));
	for (i = 0; i < ARRAY_SIZE(stats->bc); i++) {
		if (stats->bc[i])
			seq_printf(m, "%s%s: %d\n", prefix,
				   binder_command_strings[i], stats->bc[i]);
	}

	BUILD_BUG_ON(ARRAY_SIZE(stats->br) !=
		     ARRAY_SIZE(binder_return_strings));
	for (i = 0; i < ARRAY_SIZE(stats->br); i++) {
		if (stats->br[i])
			seq_printf(m, "%s%s: %d\n", prefix,
				   binder_return_strings[i], stats->br[i]);
	}

	BUILD_BUG_ON(ARRAY_SIZE(stats->obj_created) !=
		     ARRAY_SIZE(binder_objstat_strings));
	BUILD_BUG_ON(ARRAY_SIZE(stats->obj_created) !=
		     ARRAY_SIZE(stats->obj_deleted));
	for (i = 0; i < ARRAY_SIZE(stats->obj_created); i++) {
		if (stats->obj_created[i] || stats->obj_deleted[i])
			seq_printf(m, "%s%s: active %d total %d\n", prefix,
				binder_objstat_strings[i],
				stats->obj_created[i] - stats->obj_deleted[i],
				stats->obj_created[i]);
	}
}

static void print_binder_proc_stats(struct seq_file *m,
				    struct binder_proc *proc)
{
	struct binder_work *w;
	struct rb_node *n;
	int count, strong, weak;

	seq_printf(m, "proc %d\n", proc->pid);
	count = 0;
	for (n = rb_first(&proc->threads); n != NULL; n = rb_next(n))
		count++;
	seq_printf(m, "  threads: %d\n", count);
	seq_printf(m, "  requested threads: %d+%d/%d\n"
			"  ready threads %d\n"
			"  free async space %zd\n", proc->requested_threads,
			proc->requested_threads_started, proc->max_threads,
			proc->ready_threads, proc->free_async_space);
	count = 0;
	for (n = rb_first(&proc->nodes); n != NULL; n = rb_next(n))
		count++;
	seq_printf(m, "  nodes: %d\n", count);
	count = 0;
	strong = 0;
	weak = 0;
	for (n = rb_first(&proc->refs_by_desc); n != NULL; n = rb_next(n)) {
		struct binder_ref *ref = rb_entry(n, struct binder_ref,
						  rb_node_desc);
		count++;
		strong += ref->strong;
		weak += ref->weak;
	}
	seq_printf(m, "  refs: %d s %d w %d\n", count, strong, weak);

	count = 0;
	for (n = rb_first(&proc->allocated_buffers); n != NULL; n = rb_next(n))
		count++;
	seq_printf(m, "  buffers: %d\n", count);

	count = 0;
	list_for_each_entry(w, &proc->todo, entry) {
		switch (w->type) {
		case BINDER_WORK_TRANSACTION:
			count++;
			break;
		default:
			break;
		}
	}
	seq_printf(m, "  pending transactions: %d\n", count);

	print_binder_stats(m, "  ", &proc->stats);
}


static int binder_state_show(struct seq_file *m, void *unused)
{
	struct binder_proc *proc;
	struct hlist_node *pos;
	struct binder_node *node;
	int do_lock = !binder_debug_no_lock;

	if (do_lock)
		binder_lock(__func__);

	seq_puts(m, "binder state:\n");

	if (!hlist_empty(&binder_dead_nodes))
		seq_puts(m, "dead nodes:\n");
	hlist_for_each_entry(node, pos, &binder_dead_nodes, dead_node)
		print_binder_node(m, node);

	hlist_for_each_entry(proc, pos, &binder_procs, proc_node)
		print_binder_proc(m, proc, 1);
	if (do_lock)
		binder_unlock(__func__);
	return 0;
}

static int binder_stats_show(struct seq_file *m, void *unused)
{
	struct binder_proc *proc;
	struct hlist_node *pos;
	int do_lock = !binder_debug_no_lock;

	if (do_lock)
		binder_lock(__func__);

	seq_puts(m, "binder stats:\n");

	print_binder_stats(m, "", &binder_stats);

	hlist_for_each_entry(proc, pos, &binder_procs, proc_node)
		print_binder_proc_stats(m, proc);
	if (do_lock)
		binder_unlock(__func__);
	return 0;
}

static int binder_transactions_show(struct seq_file *m, void *unused)
{
	struct binder_proc *proc;
	struct hlist_node *pos;
	int do_lock = !binder_debug_no_lock;

	if (do_lock)
		binder_lock(__func__);

	seq_puts(m, "binder transactions:\n");
	hlist_for_each_entry(proc, pos, &binder_procs, proc_node)
		print_binder_proc(m, proc, 0);
	if (do_lock)
		binder_unlock(__func__);
	return 0;
}

static int binder_proc_show(struct seq_file *m, void *unused)
{
	struct binder_proc *proc = m->private;
	int do_lock = !binder_debug_no_lock;

	if (do_lock)
		binder_lock(__func__);
	seq_puts(m, "binder proc state:\n");
	print_binder_proc(m, proc, 1);
	if (do_lock)
		binder_unlock(__func__);
	return 0;
}

static void print_binder_transaction_log_entry(struct seq_file *m,
					struct binder_transaction_log_entry *e)
{
	seq_printf(m,
		   "%d: %s from %d:%d to %d:%d node %d handle %d size %d:%d\n",
		   e->debug_id, (e->call_type == 2) ? "reply" :
		   ((e->call_type == 1) ? "async" : "call "), e->from_proc,
		   e->from_thread, e->to_proc, e->to_thread, e->to_node,
		   e->target_handle, e->data_size, e->offsets_size);
}

static int binder_transaction_log_show(struct seq_file *m, void *unused)
{
	struct binder_transaction_log *log = m->private;
	int i;

	if (log->full) {
		for (i = log->next; i < ARRAY_SIZE(log->entry); i++)
			print_binder_transaction_log_entry(m, &log->entry[i]);
	}
	for (i = 0; i < log->next; i++)
		print_binder_transaction_log_entry(m, &log->entry[i]);
	return 0;
}

static const struct file_operations binder_fops = {
	.owner = THIS_MODULE,
	.poll = binder_poll,
	.unlocked_ioctl = binder_ioctl,
	.mmap = binder_mmap,
	.open = binder_open,
	.flush = binder_flush,
	.release = binder_release,
};

static struct miscdevice binder_miscdev = {
	.minor = MISC_DYNAMIC_MINOR,
	.name = "binder",
	.fops = &binder_fops
};

BINDER_DEBUG_ENTRY(state);
BINDER_DEBUG_ENTRY(stats);
BINDER_DEBUG_ENTRY(transactions);
BINDER_DEBUG_ENTRY(transaction_log);

/***************************************************/
/********** END 调试函数，可以略过, END ************/
/***************************************************/

static int __init binder_init(void)
{
	int ret;

	binder_deferred_workqueue = create_singlethread_workqueue("binder");
	if (!binder_deferred_workqueue)
		return -ENOMEM;

	binder_debugfs_dir_entry_root = debugfs_create_dir("binder", NULL);
	if (binder_debugfs_dir_entry_root)
		binder_debugfs_dir_entry_proc = debugfs_create_dir("proc",
						 binder_debugfs_dir_entry_root);
	ret = misc_register(&binder_miscdev);
	if (binder_debugfs_dir_entry_root) {
		debugfs_create_file("state",
				    S_IRUGO,
				    binder_debugfs_dir_entry_root,
				    NULL,
				    &binder_state_fops);
		debugfs_create_file("stats",
				    S_IRUGO,
				    binder_debugfs_dir_entry_root,
				    NULL,
				    &binder_stats_fops);
		debugfs_create_file("transactions",
				    S_IRUGO,
				    binder_debugfs_dir_entry_root,
				    NULL,
				    &binder_transactions_fops);
		debugfs_create_file("transaction_log",
				    S_IRUGO,
				    binder_debugfs_dir_entry_root,
				    &binder_transaction_log,
				    &binder_transaction_log_fops);
		debugfs_create_file("failed_transaction_log",
				    S_IRUGO,
				    binder_debugfs_dir_entry_root,
				    &binder_transaction_log_failed,
				    &binder_transaction_log_fops);
	}
	return ret;
}

device_initcall(binder_init);

#define CREATE_TRACE_POINTS
#include "binder_trace.h"

MODULE_LICENSE("GPL v2");
