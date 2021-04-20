package net.yzx66.rpc.converter.toString;

import java.text.SimpleDateFormat;

public class Date2StringConverter implements ToStringConverter {

    @Override
    public String convert(Object obj) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(obj);
    }
}
