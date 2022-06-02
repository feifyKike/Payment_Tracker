import java.io.*;
import java.io.File;
import java.util.*;
import java.lang.*;
import java.util.InputMismatchException;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class PaymentTracker {
    /*
     ** FEATURES**
     * Dashboard Set-up
     * 1. Display all fees subcriptions in a list and organize based on category
     * and most expensive.
     * 2. Have some stats: spent so far, monthly pay, approaching payments (next 7
     * days agenda),
     * meets budget (amount left over after spendature), suggestions on where to
     * save (3 factors: category amount, largest spent, importance), number of sub-
     * scriptions
     * 3. Last line is the actions input.
     * 
     * Followed by stats:
     * # of payments (monthly, yearly) [COMPLETE]
     * approaching payments (next 7 days) [COMPLETE]
     * meets budget and left over amount [COMPLETE]
     * suggestion with option to accept (if below budget) [COMPLETE]
     */
    private static final File data = new File("trackerData.csv");
    private static final File stats = new File("stats.csv");
    private static ArrayList<Fee> fees = new ArrayList<>();
    private static ArrayList<ArrayList<Fee>> combinations = new ArrayList<>();
    private static ArrayList<String> toDrop = new ArrayList<>();
    private static final int updateIndice = 0;
    private static final int budgetIndice = updateIndice + 1;
    private static String lastUpdate;
    private static double budget;

    static {
        try {
            data.createNewFile();
            if (stats.createNewFile()) {
                lastUpdate = LocalDate.now().toString();
                budget = 0.0;
            }

        } catch (IOException e) {
            System.out.println("Cannot find or create necessary storage files.");
        }
        try {
            if (stats.length() != 0) {
                BufferedReader br = new BufferedReader(new FileReader("stats.csv"));
                String statsLine = br.readLine();
                String[] arr = statsLine.split(", ");
                br.close();
                lastUpdate = arr[updateIndice];
                budget = Double.valueOf(arr[budgetIndice]);
            }
        } catch (IOException e) {
            System.out.println("Could not gather stats.");
        }
    }

    public static void sumComb(ArrayList<Fee> arr, double target, ArrayList<Fee> partial) {
        double sum = 0.0;
        for (Fee fee : partial) {
            sum += fee.getAmount();
        }
        if (sum >= target) {
            combinations.add(partial);
            return;
        }
        for (int i = 0; i < arr.size(); i++) {
            ArrayList<Fee> remaining = new ArrayList<>();
            Fee val = arr.get(i);
            for (int j = i + 1; j < arr.size(); j++) {
                remaining.add(arr.get(j));
            }
            ArrayList<Fee> partial_rec = new ArrayList<Fee>(partial);
            partial_rec.add(val);
            sumComb(remaining, target, partial_rec);
        }
    }

    public static ArrayList<Fee> save() {
        /*
         * REQUIREMENT: Has to be over-budget
         * - Create new list with only monthly payments
         * - [subset sum algorithm] Find all combinations of prices per month that meet
         * margin value (spent - budget) = + val
         * - Choose subset with lowest rank sum and number in group
         */
        combinations.clear();
        ArrayList<Fee> onlyMonthly = new ArrayList<>();
        for (Fee fee : fees) {
            if (fee.getFreq().equals("monthly")) {
                onlyMonthly.add(fee);
            }
        }
        sumComb(onlyMonthly, spent()[0] - budget, new ArrayList<Fee>());

        double min = Integer.MAX_VALUE;
        double closest = Integer.MAX_VALUE;
        int minIndex = 0;
        for (int i = 0; i < combinations.size(); i++) {
            int curr = 0;
            double curr2 = 0.0;
            for (Fee fee : combinations.get(i)) {
                curr += fee.getRank();
                curr2 += fee.getAmount();
            }
            double average = curr / combinations.size();
            if (average <= min && curr2 < closest) {
                min = curr;
                closest = curr2;
                minIndex = i;
            }
        }
        return combinations.get(minIndex);

    }

    public static String approachingPay() {
        StringJoiner approachingDates = new StringJoiner("\n * ", "\n * ", "");
        for (Fee fee : fees) {
            String date = fee.getPurchaseDate();
            int month = Integer.parseInt(date.substring(0, 2));
            int day = Integer.parseInt(date.substring(3, 5));
            int year = Integer.parseInt(date.substring(6, date.length()));
            Period diff = Period.between(LocalDate.of(year, month, day), LocalDate.now());
            if (diff.getDays() <= 7) {
                approachingDates.add(fee.getName() + " ("
                        + LocalDate.now().plusDays(diff.getDays()).toString().toLowerCase() + ")");
            }
        }
        if (approachingDates.toString().length() == 0) {
            return "* None Upcoming.";
        }
        return approachingDates.toString();
    }

    public static Double[] spent() {
        // {monthly, yearly}
        Double[] spending = { 0.0, 0.0 };
        for (Fee fee : fees) {
            if (fee.getFreq().equals("monthly")) {
                spending[0] += fee.getAmount();
                spending[1] += fee.getAmount() * 12.0;
            } else if (fee.getFreq().equals("yearly")) {
                spending[0] += fee.getAmount() / 12.0;
                spending[1] += fee.getAmount();
            }
        }
        spending[0] = Math.round(spending[0] * 100.0) / 100.0;
        spending[1] = Math.round(spending[1] * 100.0) / 100.0;
        return spending;
    }

    public static String meetsBudget(double spent) {
        if (budget > 0 && budget < spent) {
            double margin = Math.round((spent - budget) * 100.00) / 100.0;
            return "[👎 Budget not met | " + "$" + margin + " over]";
        } else if (budget > 0 && budget > spent) {
            double margin = Math.round((budget - spent) * 100.0) / 100.0;
            return "[👍 Budget met | " + "$" + margin + " below]";
        }
        return "[No budget set]";
    }

    public static void updateBudget() throws IOException {
        FileWriter out = new FileWriter(stats.getName());
        PrintWriter write = new PrintWriter(out);
        write.println(LocalDate.now() + ", " + budget);
        write.close();
    }

    public static void rewrite() throws IOException {
        // add entire list to file (rewrite without line)
        FileWriter out = new FileWriter(data.getName());
        PrintWriter write = new PrintWriter(out);
        String toAdd = "";
        StringJoiner toJoin;
        for (int i = 0; i < fees.size(); i++) {
            toJoin = new StringJoiner(", ");
            toJoin.add(fees.get(i).getName()).add(String.valueOf(fees.get(i).getAmount())).add(fees.get(i).getFreq())
                    .add(fees.get(i).getPurchaseDate()).add(fees.get(i).getCategory())
                    .add(String.valueOf(fees.get(i).getRank()));
            if (i == fees.size() - 1) {
                toAdd += toJoin.toString();
            } else {
                toAdd += toJoin.toString() + "\n";
            }

        }
        write.println(toAdd);
        write.close();
    }

    public static void display() throws IOException {
        // display stats
        Double[] spendings = spent();
        System.out
                .println(fees.size() + " subscriptions | " + "$" + spendings[0] + "/M, " + "$" + spendings[1] + "/YR");
        // System.out.println("Total Spent: " + "$" + spentTotal);
        System.out.println("Approaching Payments: " + approachingPay());
        System.out.println("---");

        // display list
        if (fees.size() == 0) {
            System.out.println("Nothing to show - Empty");
        }

        ArrayList<String> cs = new ArrayList<String>();
        for (Fee fee : fees) {
            if (!cs.contains(fee.getCategory())) {
                cs.add(fee.getCategory());
            }
        }
        cs.sort(String::compareToIgnoreCase);
        for (String category : cs) {
            System.out.println(category.toUpperCase());
            for (Fee fee : fees) {
                if (fee.getCategory().equals(category)) {
                    System.out.println(fee);
                }
            }
            System.out.println();
        }
        // budget
        System.out.println(meetsBudget(spendings[0]));

        // suggestions
        if (spent()[0] >= budget * 2.0) {
            System.out.println("\nSuggestion: Budget set too low.");
        } else if (spent()[0] > budget) {
            System.out.print("\nSuggestion: In order to meet your set budget you can drop ");
            ArrayList<Fee> printList = save();
            for (int i = 0; i < printList.size(); i++) {
                toDrop.add(printList.get(i).getName());
                System.out.print(printList.get(i).getName());
                if (i != printList.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }

    }

    public static void main(String[] args) {
        try {
            System.out.println();
            if (data.length() == 0) {
                System.out.println("Nothing to show - Empty");
            } else {
                Scanner br = new Scanner(data);
                String[] tempArr;
                while (br.hasNextLine()) {
                    tempArr = br.nextLine().split(", ");
                    fees.add(new Fee(tempArr[0], Double.parseDouble(tempArr[1]), tempArr[2], tempArr[3], tempArr[4],
                            Integer.parseInt(tempArr[5])));
                }
                br.close();
                fees.sort(Collections.reverseOrder());
                display();
            }

            // command line
            Scanner input = new Scanner(System.in);
            while (true) {
                System.out.print("\nAction (add, edit, remove, budget, suggestion, quit): ");
                String action = input.nextLine();
                Fee fee;
                if (action.equals("quit")) {
                    break;
                } else if (action.equals("add")) {
                    System.out.print("Name: ");
                    String name = input.nextLine();
                    System.out.print("Price: ");
                    double price = input.nextDouble();
                    input.nextLine();
                    System.out.print("Payment Frequency: ");
                    String freq = input.nextLine();
                    System.out.print("Purchase Date: ");
                    String date = input.nextLine();
                    System.out.print("Category: ");
                    String category = input.nextLine();
                    System.out.print("Level of Importance (1 - 5): ");
                    int importance = input.nextInt();
                    input.nextLine();
                    fee = new Fee(name, price, freq, date, category.toLowerCase(), importance);
                    fees.add(fee);
                    fee.record();
                } else if (action.equals("edit")) {
                    System.out.print("Original Name: ");
                    String originName = input.nextLine();
                    System.out.print("Name: ");
                    String newName = input.nextLine();
                    System.out.print("Price: ");
                    double newPrice = input.nextDouble();
                    input.nextLine();
                    System.out.print("Payment Frequency: ");
                    String newFreq = input.nextLine();
                    System.out.print("Purchase Date: ");
                    String newDate = input.nextLine();
                    System.out.print("Category: ");
                    String newCategory = input.nextLine();
                    System.out.print("Level of Importance (1 - 5): ");
                    int newImportance = input.nextInt();
                    input.nextLine();
                    for (int i = 0; i < fees.size(); i++) {
                        if (fees.get(i).getName().equals(originName)) {
                            fees.get(i).setName(newName);
                            fees.get(i).setAmount(newPrice);
                            fees.get(i).setFreq(newFreq);
                            fees.get(i).setPurchaseDate(newDate);
                            fees.get(i).setCategory(newCategory.toLowerCase());
                            fees.get(i).setRank(newImportance);
                        }
                    }
                    rewrite();
                    System.out.println("🔨 Edited " + originName);
                } else if (action.equals("remove")) {
                    // remove item from fees
                    System.out.print("Name: ");
                    String name = input.nextLine();
                    for (int i = 0; i < fees.size(); i++) {
                        if (fees.get(i).getName().equals(name)) {
                            fees.remove(i);
                        }
                    }
                    rewrite();
                    System.out.println("⛔️ Removed " + name);
                } else if (action.equals("budget")) {
                    System.out.print("New Budget: ");
                    double newBudget = input.nextDouble();
                    input.nextLine();
                    budget = newBudget;
                    updateBudget();
                    System.out.println("✅ Successful");
                } else if (action.equals("suggestion")) {
                    if (toDrop.size() != 0) {
                        for (String dropFee : toDrop) {
                            for (int i = 0; i < fees.size(); i++) {
                                if (fees.get(i).getName().equals(dropFee)) {
                                    fees.remove(i);
                                }
                            }
                        }
                        rewrite();
                        System.out.println("✅ Suggestion Accepted");
                    } else {
                        System.out.println("No suggestion to Accept");
                    }
                    
                } else {
                    System.out.println("Please input correct values!");
                    continue;
                }
                System.out.println();
                fees.sort(Collections.reverseOrder());
                display();
            }
            input.close();

        } catch (IOException e) {
            System.out.println("Error: Can not display data.");
        } catch (InputMismatchException e) {
            System.out.println("Error: Value input error.");
        }
    }
}
