package com.spotifymod.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class OAuthCallbackServer {
    private HttpServer server;
    private CompletableFuture<String> authCodeFuture;

    public CompletableFuture<String> start() {
        authCodeFuture = new CompletableFuture<>();

        try {
            server = HttpServer.create(new InetSocketAddress(8888), 0);
            server.createContext("/callback", new CallbackHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("OAuth callback server started on port 8888");
        } catch (IOException e) {
            e.printStackTrace();
            authCodeFuture.completeExceptionally(e);
        }

        return authCodeFuture;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("OAuth callback server stopped");
        }
    }

    private class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String authCode = null;

            if (query != null && query.contains("code=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("code=")) {
                        authCode = param.substring(5);
                        break;
                    }
                }
            }

            String response;
            if (authCode != null) {
                response = "<html><body><h1>Authorization Successful!</h1><p>You can now close this window and return to Minecraft.</p></body></html>";
                authCodeFuture.complete(authCode);
            } else {
                response = "<html><body><h1>Authorization Failed!</h1><p>No authorization code received.</p></body></html>";
                authCodeFuture.completeExceptionally(new Exception("No auth code received"));
            }

            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

            // Stop the server after handling the callback
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
