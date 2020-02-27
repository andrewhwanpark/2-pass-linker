Lab 1
Andrew Park (hp1141)

To compile: javac lab1.java
To run: java lab1

The program will take input from keyboard using Scanner. Simply copy & paste the appropriate input. 

The program has been tested with all 9 inputs provided by Professor Yan. All 9 inputs produced correct outputs.

The program utilizes OOP using 3 main objects: Module, Symbol, and Text. 

The program extensively uses ArrayList from the Java library. The choice was made due to the flexible nature of ArrayList, and the decent runtimes of different operations such as retrival, add, and more.

The arbitrary limit of Symbol length is 8. If a Symbol is longer than 8 characters, the program will produce a warning at the end. However, the program will not cut the string short or modify the string. The program will proceed without modifications after the warning.

During input read, the program will calculate the base address of each module.

During pass 1, the symbol table will be created & stored. Moreover, the absolute address of each definition will be calculated.

During pass 2, the memory map will be created & stored. The symbol table and memory map will be printed during the pass. 

Errors are caught during both passes.
