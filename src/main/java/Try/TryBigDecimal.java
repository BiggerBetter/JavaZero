package Try;

import com.sun.xml.internal.ws.api.model.wsdl.WSDLOutput;
import org.w3c.dom.ls.LSOutput;

import java.math.BigDecimal;

public class TryBigDecimal {
    public static void main(String[] args) {
        BigDecimal b1 = new BigDecimal("123.45");
        BigDecimal b2 = new BigDecimal("456.67");

        //加减乘除
        BigDecimal cheng_fa = b1.multiply(b2);
        BigDecimal chufa = b1.divide(b2,1);
        BigDecimal jiafa = b1.add(b2);
        BigDecimal jainfa = b1.subtract(b2);

        //转成double
        Double dable = b1.doubleValue();

        //转成double
        int innt = b1.intValue();
    }


}
