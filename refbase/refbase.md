```dot
@startuml refbase-diagram.png
skinparam defaultFontName "Consolas"
scale 1.5

/' RefBase '/
class **RefBase** {
    - weakref_impl* const **mRefs**;
    ===
    +void           **incStrong**(const void* id) const;
    +void           **decStrong**(const void* id) const;
    +void           **forceIncStrong**(const void* id) const;
    +weakref_type*  **createWeak**(const void* id) const;
    +weakref_type*  **getWeakRefs**() const;
    +void           **extendObjectLifetime**(int32_t mode);
    ---
    +int32_t        getStrongCount() const; // debug only
    ---
    #**RefBase**();
    #virtual        **~RefBase**();
    #virtual void   **onFirstRef**();
    #virtual void   **onLastStrongRef**(const void* id);
    #virtual bool   **onIncStrongAttempted**(uint32_t flags, const void* id);
}

class RefBase::**weakref_impl** {
    -volatile int32_t  **mStrong**;
    -volatile int32_t  **mWeak**;
    -RefBase* const    **mBase**;
    -volatile int32_t  **mFlags**;
    ===
    +RefBase*  **refBase**() const;
    +void      **incWeak**(const void* id);
    +void      **decWeak**(const void* id);
    +bool      **attemptIncStrong**(const void* id);
    +bool      **attemptIncWeak**(const void* id);
    ---
    +int32_t   getWeakCount() const; // debug only
}

class **sp** {
    - T* **m_ptr**;
    ===
    **...overridden operators...**
    ---
    +**constructor()**
    +**desctructor()**
    ---
    +inline T*  **get**() const;
    +void       **clear**();
}

class **wp** {
    -T*             **m_ptr**;
    -weakref_type*  **m_refs**;
    ===
    **...overridden operators...**
    ---
    +**constructor()**
    +**desctructor()**
    ---
    +void           **set_object_and_refs**(T* other, weakref_type* refs);
    +sp<T>          **promote**() const;
    +T*             **unsafe_get**() const;
    +weakref_type*  **get_refs**() const;
    +void           **clear**();
}
@enduml
```