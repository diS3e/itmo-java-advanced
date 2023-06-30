open module module.samodelov {
    requires transitive info.kgeorgiy.java.advanced.implementor;
    requires transitive info.kgeorgiy.java.advanced.student;
    requires transitive info.kgeorgiy.java.advanced.concurrent;
    requires transitive info.kgeorgiy.java.advanced.mapper;
    requires transitive info.kgeorgiy.java.advanced.crawler;
    requires transitive info.kgeorgiy.java.advanced.hello;
    requires java.compiler;
    requires junit;

    exports info.kgeorgiy.ja.samodelov.arrayset;
    exports info.kgeorgiy.ja.samodelov.implementor;
    exports info.kgeorgiy.ja.samodelov.student;
    exports info.kgeorgiy.ja.samodelov.walk;
    exports info.kgeorgiy.ja.samodelov.concurrent;
}