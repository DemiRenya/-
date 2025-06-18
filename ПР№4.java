import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class DataItem {
    private String key;
    private String value;
    private boolean readOnly;

    public DataItem(String key, String value, boolean readOnly) {
        this.key = key;
        this.value = value;
        this.readOnly = readOnly;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public boolean isReadOnly() { return readOnly; }

    public void setValue(String value) { this.value = value; }

    @Override
    public String toString() {
        return "DataItem{key='" + key + "', value='" + value + "', readOnly=" + readOnly + "}";
    }
}

class DataService {
    // Имитация БД
    private final Map<String, DataItem> database = new HashMap<>();

    // Кэш для read-only данных
    private final Map<String, DataItem> cache = new ConcurrentHashMap<>();

    // Получить данные по ключу
    public DataItem getByKey(String key) {
        if (cache.containsKey(key)) {
            System.out.println("Возвращаем из кэша");
            return cache.get(key);
        }
        DataItem item = database.get(key);
        if (item != null && item.isReadOnly()) {
            System.out.println("Кэшируем read-only элемент");
            cache.put(key, item);
        }
        return item;
    }

    // Сохранить или обновить данные
    public void save(DataItem item) {
        if (item.isReadOnly()) {
            throw new IllegalArgumentException("Read-only данные нельзя изменять");
        }
        database.put(item.getKey(), item);
        System.out.println("Данные сохранены в базе");
    }

    // Добавить данные напрямую в базу (имитация первоначальной загрузки)
    public void load(DataItem item) {
        database.put(item.getKey(), item);
    }
}

public class Main {
    public static void main(String[] args) {
        DataService service = new DataService();

        // Инициализируем "БД" данными
        service.load(new DataItem("key1", "value1", true));  // read-only
        service.load(new DataItem("key2", "value2", false)); // изменяемый

        // Запрашиваем key1 - сначала из базы, потом кэшируем
        System.out.println(service.getByKey("key1"));
        System.out.println(service.getByKey("key1"));  // уже из кэша

        // Запрашиваем key2 - всегда из базы
        System.out.println(service.getByKey("key2"));

        // Пытаемся обновить key2 - должно пройти
        service.save(new DataItem("key2", "newValue2", false));
        System.out.println(service.getByKey("key2"));

        // Пытаемся обновить key1 (read-only) - ошибка
        try {
            service.save(new DataItem("key1", "newValue1", true));
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}
