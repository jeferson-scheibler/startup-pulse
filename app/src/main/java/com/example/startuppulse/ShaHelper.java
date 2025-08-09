package com.example.startuppulse;

import java.security.MessageDigest;

public class ShaHelper {

    // Metodo para gerar o hash SHA-256 de uma string
    public static String gerarHash(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(texto.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash SHA-256", e);
        }
    }

    // Metodo de comparação de hash (exemplo prático)
    public boolean verificarHash(String textoOriginal, String hashEsperado) {
        String novoHash = gerarHash(textoOriginal);
        return novoHash.equalsIgnoreCase(hashEsperado);
    }
}

