import java.util.*;
import java.io.*;
import java.lang.*;

public class Fee implements Comparable<Fee> {
    private String name;
    private double amount;
    private String freq;
    private String category;
    private String purchaseDate;
    private int importance;

    /*
     * Individual Subscription
     * 1. Each subscription: name, category, amount, freq, pic link, edit, and
     * remove.
     * 2. Display: Youtube - $15.00/monthly
     */
    public Fee(String name, double amount, String freq, String purchaseDate, String category, int rank) {
        this.name = name;
        this.amount = amount;
        this.freq = freq;
        this.purchaseDate = purchaseDate;
        this.category = category;
        importance = rank; // integer between 1 - 5
    }

    // getters
    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public double getAmount() {
        return amount;
    }

    public String getFreq() {
        return freq;
    }

    public int getRank() {
        return importance;
    }

    // setters
    public void setName(String n) {
        name = n;
    }

    public void setCategory(String c) {
        category = c;
    }

    public void setPurchaseDate(String date) {
        purchaseDate = date;
    }

    public void setAmount(double price) {
        amount = price;
    }

    public void setFreq(String f) {
        freq = f;
    }

    public void setRank(int r) {
        importance = r;
    }

    // behavior methods
    public boolean existsCSV() throws IOException {
        // checks fee_data.json and determines if namespace not taken
        BufferedReader br = new BufferedReader(new FileReader(new File("trackerData.csv")));
        String[] toConcat;
        String line = "";
        while ((line = br.readLine()) != null) {
            toConcat = line.split(", ");
            if (toConcat[0].toLowerCase().equals(name.toLowerCase()) && toConcat[3].equals(purchaseDate)) {
                br.close();
                return true;
            }
        }
        br.close();
        return false;

    }

    public void existsJSON() {
        // to be implemented
    }

    public void record() throws IOException {
        // to be implemented
        try {
            FileWriter out = new FileWriter("trackerData.csv", true);
            PrintWriter write = new PrintWriter(out);
            if (!existsCSV()) {
                StringJoiner build = new StringJoiner(", ");
                build.add(name).add(String.valueOf(amount)).add(freq).add(purchaseDate).add(category)
                        .add(String.valueOf(importance));
                write.println(build.toString());
                System.out.println("✅ Successful");
            } else {
                System.out.println("Already exists");
            }
            write.close();
        } catch (Exception e) {
            System.out.println("❌ Unsuccessful");
        }
    }

    public int compareTo(Fee fee) {
        if (amount == fee.getAmount()) {
            return 0;
        } else if (amount > fee.getAmount()) {
            return 1;
        }
        return -1;
    }

    public String toString() {
        return name + " - " + "$" + amount + "/" + freq;
    }
}
