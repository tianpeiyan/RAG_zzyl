package com.zzyl.test;

import org.junit.Ignore;

@Ignore
public class ThreadLocalTest {

    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();//内部结构，就是一个map


    public static void main(String[] args) {

        Thread t1 = new Thread(() -> {
            THREAD_LOCAL.set("itheima");
            getData("t1");
        }, "t1");

        Thread t2 = new Thread(() -> {
            THREAD_LOCAL.set("itcast");
            getData("t2");
        }, "t2");

        t1.start();
        t2.start();

    }

    private static void getData(String threadName){
        Object data = THREAD_LOCAL.get();
        System.out.println(threadName+"-"+data);
    }
}
