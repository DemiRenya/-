import java.util.*;
import java.util.regex.*;

public class Main {

    static class ExpressionEvaluator {
        private static final Set<String> OPERATORS = Set.of("+", "-", "*", "/", "^", "**", "!");
        private static final Set<String> FUNCTIONS = Set.of("log", "exp");

        private static final Map<String, Integer> PRECEDENCE = Map.ofEntries(
            Map.entry("!", 5),       // самый высокий приоритет (факториал)
            Map.entry("**", 4),      // степень (правоассоциативный)
            Map.entry("^", 4),       // степень (правоассоциативный)
            Map.entry("*", 3),
            Map.entry("/", 3),
            Map.entry("+", 2),
            Map.entry("-", 2)
        );

        // Проверка баланса скобок
        public boolean checkBracketsBalance(String expr) {
            int count = 0;
            for (char c : expr.toCharArray()) {
                if (c == '(') count++;
                else if (c == ')') {
                    count--;
                    if (count < 0) return false;
                }
            }
            return count == 0;
        }

        public boolean validateExpression(String expr) {
            expr = expr.trim();
            if (!checkBracketsBalance(expr)) return false;

            // Начало выражения: число, унарный минус, функция или скобка
            if (!expr.matches("^(-?\\d|log\\(|exp\\(|\\().*")) return false;

            // Конец выражения: число, факториал или закрывающая скобка
            if (!expr.matches(".*(\\d|!|\\))$")) return false;

            int termsCount = countTerms(expr);
            if (termsCount > 15) return false;

            return true;
        }

        private int countTerms(String expr) {
            // Подсчёт верхнеуровневых слагаемых (учитываем скобки)
            int count = 1;
            int depth = 0;
            for (int i = 0; i < expr.length(); i++) {
                char c = expr.charAt(i);
                if (c == '(') depth++;
                else if (c == ')') depth--;
                else if ((c == '+' || c == '-') && depth == 0) {
                    if (i > 0) {
                        char prev = expr.charAt(i - 1);
                        if (prev != '+' && prev != '-' && prev != '*' && prev != '/' && prev != '^' && prev != '(') {
                            count++;
                        }
                    }
                }
            }
            return count;
        }

        public double evaluate(String expr) throws Exception {
            List<String> tokens = tokenize(expr);
            List<String> rpn = infixToRPN(tokens);
            return evalRPN(rpn);
        }

        private List<String> tokenize(String expr) throws Exception {
            List<String> tokens = new ArrayList<>();
            int i = 0;
            while (i < expr.length()) {
                char c = expr.charAt(i);

                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }

                // Функции log, exp
                if (expr.startsWith("log(", i)) {
                    tokens.add("log");
                    i += 3;
                    continue;
                }
                if (expr.startsWith("exp(", i)) {
                    tokens.add("exp");
                    i += 3;
                    continue;
                }

                // Число с возможным унарным минусом
                if ((c == '-' && (i == 0 || expr.charAt(i - 1) == '(' || OPERATORS.contains(String.valueOf(expr.charAt(i - 1))) || expr.charAt(i - 1) == ','))) {
                    int start = i;
                    i++;
                    while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i++;
                    tokens.add(expr.substring(start, i));
                    continue;
                }

                // Число
                if (Character.isDigit(c) || c == '.') {
                    int start = i;
                    while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i++;
                    tokens.add(expr.substring(start, i));
                    continue;
                }

                // Два символа - оператор **?
                if (c == '*' && i + 1 < expr.length() && expr.charAt(i + 1) == '*') {
                    tokens.add("**");
                    i += 2;
                    continue;
                }

                // Операторы + - * / ^ !
                if ("+-*/^()!".indexOf(c) != -1) {
                    tokens.add(String.valueOf(c));
                    i++;
                    continue;
                }

                throw new Exception("Неизвестный символ: " + c);
            }

            return tokens;
        }

        private List<String> infixToRPN(List<String> tokens) throws Exception {
            List<String> output = new ArrayList<>();
            Deque<String> stack = new ArrayDeque<>();

            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);

                if (isNumber(token)) {
                    output.add(token);
                } else if (FUNCTIONS.contains(token)) {
                    stack.push(token);
                } else if (token.equals("(")) {
                    stack.push(token);
                } else if (token.equals(")")) {
                    while (!stack.isEmpty() && !stack.peek().equals("(")) {
                        output.add(stack.pop());
                    }
                    if (stack.isEmpty()) throw new Exception("Несбалансированные скобки");
                    stack.pop(); // убираем "("

                    // Если сверху функция - выталкиваем её в output
                    if (!stack.isEmpty() && FUNCTIONS.contains(stack.peek())) {
                        output.add(stack.pop());
                    }
                } else if (OPERATORS.contains(token)) {
                    if (token.equals("!")) {
                        // Факториал — унарный постфиксный оператор, сразу добавляем в output
                        output.add(token);
                        continue;
                    }

                    while (!stack.isEmpty() && OPERATORS.contains(stack.peek())) {
                        String opTop = stack.peek();
                        int prec1 = PRECEDENCE.getOrDefault(token, 0);
                        int prec2 = PRECEDENCE.getOrDefault(opTop, 0);

                        // Правоассоциативные степени
                        boolean rightAssociative = token.equals("^") || token.equals("**");

                        if ((rightAssociative && prec1 < prec2) || (!rightAssociative && prec1 <= prec2)) {
                            output.add(stack.pop());
                        } else break;
                    }
                    stack.push(token);
                } else {
                    throw new Exception("Неизвестный токен: " + token);
                }
            }

            while (!stack.isEmpty()) {
                String op = stack.pop();
                if (op.equals("(") || op.equals(")")) {
                    throw new Exception("Несбалансированные скобки");
                }
                output.add(op);
            }

            return output;
        }

        private boolean isNumber(String s) {
            try {
                Double.parseDouble(s);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private double evalRPN(List<String> tokens) throws Exception {
            Deque<Double> stack = new ArrayDeque<>();

            for (String token : tokens) {
                if (isNumber(token)) {
                    stack.push(Double.parseDouble(token));
                } else if (FUNCTIONS.contains(token)) {
                    if (stack.isEmpty()) throw new Exception("Ошибка: функция " + token + " без аргумента");
                    double arg = stack.pop();
                    switch (token) {
                        case "log" -> {
                            if (arg <= 0) throw new Exception("Логарифм аргумента <= 0");
                            stack.push(Math.log(arg) / Math.log(2)); // лог по основанию 2
                        }
                        case "exp" -> stack.push(Math.exp(arg));
                        default -> throw new Exception("Неизвестная функция: " + token);
                    }
                } else {
                    if (token.equals("!")) {
                        if (stack.isEmpty()) throw new Exception("Факториал без числа");
                        double val = stack.pop();
                        if (val < 0 || val != Math.floor(val)) throw new Exception("Факториал только для неотрицательных целых чисел");
                        stack.push((double) factorial((int) val));
                        continue;
                    }

                    if (stack.size() < 2) throw new Exception("Ошибка в выражении, оператор " + token);

                    double b = stack.pop();
                    double a = stack.pop();

                    switch (token) {
                        case "+" -> stack.push(a + b);
                        case "-" -> stack.push(a - b);
                        case "*" -> stack.push(a * b);
                        case "/" -> {
                            if (b == 0) throw new Exception("Деление на ноль");
                            stack.push(a / b);
                        }
                        case "^", "**" -> stack.push(Math.pow(a, b));
                        default -> throw new Exception("Неизвестный оператор " + token);
                    }
                }
            }

            if (stack.size() != 1) throw new Exception("Ошибка вычисления");

            return stack.pop();
        }

        private long factorial(int n) {
            long res = 1;
            for (int i = 2; i <= n; i++) res *= i;
            return res;
        }
    }

    static class ConsoleView {
        private final Scanner scanner = new Scanner(System.in);

        public String getInputExpression() {
            System.out.print("Введите математическое выражение: ");
            return scanner.nextLine();
        }

        public void showResult(double result) {
            System.out.println("Результат: " + result);
        }

        public void showError(String message) {
            System.err.println("Ошибка: " + message);
        }
    }

    static class CalculatorController {
        private final ExpressionEvaluator model;
        private final ConsoleView view;

        public CalculatorController(ExpressionEvaluator model, ConsoleView view) {
            this.model = model;
            this.view = view;
        }

        public void run() {
            try {
                String expr = view.getInputExpression();
                if (!model.validateExpression(expr)) {
                    view.showError("Выражение невалидно: проверьте баланс скобок, количество слагаемых (до 15), и корректность.");
                    return;
                }
                double result = model.evaluate(expr);
                view.showResult(result);
            } catch (Exception e) {
                view.showError(e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        ExpressionEvaluator model = new ExpressionEvaluator();
        ConsoleView view = new ConsoleView();
        CalculatorController controller = new CalculatorController(model, view);
        controller.run();
    }
}
