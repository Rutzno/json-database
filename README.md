# JSON-Database

**JSON** (_JavaScript Object Notation_) is a lightweight data-interchange format. 
It is easy for humans to read and write and for machines to parse and generate. 
It is less verbose and more readable than `XML`. The _JSON_ filename extension is `.json`. 
The official Internet media type for _JSON_ is `application/json`.  

**GSON** is a Java serialization/deserialization library to convert _Java Objects_ into 
_JSON_ and vice versa. _Gson_ was created by _Google_ for internal use and later open sourced.

[_JCommander_](http://jcommander.org) is a very small _Java_ framework that makes it trivial to parse 
command line parameters. You annotate fields with descriptions of your options.

We need synchronization because all our threads will work with the same file.
Since we can't write the file in two separate threads simultaneously, we used 
`java.util.concurrent.locks.ReentrantReadWriteLock` class, an implementation of the 
`ReadWriteLock` interface which provides a pair of read-write lock. It allows multiple readers 
of the resource but only a single writer. Once a writer locks the resource, it 
waits until all the readers finish reading and only then starts to write. 
The readers can freely read the file even though other readers locked it, but if 
the writer locks the file, no readers can read it.

`</>`