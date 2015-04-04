```dot
@startuml refbase-diagram.png
skinparam defaultFontName Consolas
scale 1.5

/' RefBase '/
class **RefBase** {
    - weakref_impl* const **mRefs**;
    ===
    void **incStrong**(const void* id) const;
    void **decStrong**(const void* id) const;
    void **forceIncStrong**(const void* id) const;
    weakref_type* **createWeak**(const void* id) const;
    weakref_type* **getWeakRefs**() const;
    void **extendObjectLifetime**(int32_t mode);
    ---
    virtual void **onFirstRef**();
    virtual void **onLastStrongRef**(const void* id);
    virtual bool **onIncStrongAttempted**(uint32_t flags, const void* id);
}
@enduml
```