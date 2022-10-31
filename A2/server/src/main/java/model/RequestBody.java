package model;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestBody {
    private String parameterName;
    private String value;
    private final Integer lowerRange;
    private final Integer upperRange;

    public RequestBody(String parameterName, String value, Integer lowerRange, Integer upperRange) {
        this.parameterName = parameterName;
        this.value = value;
        this.lowerRange = lowerRange;
        this.upperRange = upperRange;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String val) {
        this.value = val;
    }


    public boolean isValidValue(HttpServletResponse response) throws IOException{
        // convert the value into integer

        try {
            Integer val = Integer.parseInt(value);
            boolean rangeCheck = (val >= lowerRange && val < upperRange);
            if (!rangeCheck) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        return true;
    }
}
