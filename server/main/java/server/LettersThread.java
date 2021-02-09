package server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

    public class LettersThread {

        static Object mon = new Object();
        static volatile int currentNum = 1;
        static final int number = 5;

        public static void main(String[] args) {
            ExecutorService service = Executors.newFixedThreadPool(3);
            service.execute(()-> {
                try {
                    for (int i = 0; i < number; i++) {
                        synchronized (mon) {
                            while (currentNum != 1) {
                                mon.wait();
                            }
                            System.out.print("A");
                            currentNum = 2;
                            mon.notifyAll();
                        }
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            service.execute(()-> {
                try {
                    for (int i = 0; i < number; i++) {
                        synchronized (mon) {
                            while (currentNum != 2) {
                                mon.wait();
                            }
                            System.out.print("B");
                            currentNum = 3;
                            mon.notifyAll();
                        }
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            service.execute(()-> {
                try {
                    for (int i = 0; i < number; i++) {
                        synchronized (mon) {
                            while (currentNum != 3) {
                                mon.wait();
                            }
                            System.out.print("C");
                            currentNum = 1;
                            mon.notifyAll();
                        }
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            service.shutdown();

        }

    }
