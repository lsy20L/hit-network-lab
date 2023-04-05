package com.httpproxy;

import com.httpproxy.proxy.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int port =8050;
        new Server(port).start();
    }
}
