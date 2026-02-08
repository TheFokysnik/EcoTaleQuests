package com.ecotale.api;

import java.util.UUID;

/**
 * Stub API для компиляции. В рантайме используется реальный JAR Ecotale.
 * Сигнатуры должны ТОЧНО совпадать с реальным EcotaleAPI из Ecotale-1.0.7.jar.
 * @see <a href="https://github.com/Tera-bytez/Ecotale">Ecotale GitHub</a>
 */
public class EcotaleAPI {

    /**
     * Проверяет, инициализирован ли API.
     */
    public static boolean isAvailable() {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Получить баланс игрока. NOT rate limited.
     */
    public static double getBalance(UUID playerUuid) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Проверить, есть ли у игрока достаточно средств. NOT rate limited.
     */
    public static boolean hasBalance(UUID playerUuid, double amount) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Начислить средства. Rate limited.
     * @return true если успешно, false если отклонено (макс. баланс, невалидная сумма)
     * @throws RuntimeException (EcotaleRateLimitException) при превышении лимита
     */
    public static boolean deposit(UUID playerUuid, double amount, String reason) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Списать средства. Rate limited.
     * @return true если успешно, false если недостаточно средств
     * @throws RuntimeException (EcotaleRateLimitException) при превышении лимита
     */
    public static boolean withdraw(UUID playerUuid, double amount, String reason) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Форматировать сумму с символом валюты. NOT rate limited.
     */
    public static String format(double amount) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Получить символ валюты. NOT rate limited.
     */
    public static String getCurrencySymbol() {
        throw new UnsupportedOperationException("Stub");
    }
}
