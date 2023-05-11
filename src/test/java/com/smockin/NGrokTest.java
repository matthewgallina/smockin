package com.smockin;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;

public class NGrokTest {

    public static void main(String[] args) {

        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(8001)
                .build();

        final NgrokClient ngrokClient = new NgrokClient
                .Builder()
                .build();

        // Open a HTTP tunnel on the default port 80
        // <Tunnel: "http://<public_sub>.ngrok.io" -> "http://localhost:80">
        final Tunnel httpTunnel = ngrokClient.connect(createTunnel);

    }

}
