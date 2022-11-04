import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class DataProcessor {
    private String path;
    private BufferedReader reader;
    private ArrayList result = new ArrayList();

    DataProcessor(String path) throws IOException {
        this.path = path;
        reader = new BufferedReader(new FileReader(this.path));
    }

    public void readFromFile() throws IOException {
        String line;
        while ((line = reader.readLine()) != null ) {
            int latency = Integer.valueOf(line.split(",")[2]);
            this.result.add(latency);
        }
        this.result.sort(Comparator.naturalOrder());
    }

    public int getMean() {
        return this.getSum()/this.result.size();
    }

    public int getMedian() {
        return (int) this.result.get(this.result.size()/2);
    }

    public int getMax() {
        return (int) this.result.get(this.result.size()-1);
    }

    public int getP99() {
        return (int) this.result.get(this.result.size()/100 * 99);
    }

    public int getSum() {
        int total = 0;
        for (Object n: this.result){
            total += (int)n;
        }
        return total;
    }
}

