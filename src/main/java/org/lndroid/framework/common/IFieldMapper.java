package org.lndroid.framework.common;

import org.lndroid.framework.WalletData;

import java.util.List;

public interface IFieldMapper<DataType> {
    List<WalletData.Field> mapToFields(DataType t);
}
