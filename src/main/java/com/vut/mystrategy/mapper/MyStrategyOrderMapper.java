package com.vut.mystrategy.mapper;

import com.vut.mystrategy.entity.MyStrategyOrder;
import com.vut.mystrategy.model.MyStrategyOrderRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MyStrategyOrderMapper {
    MyStrategyOrderMapper INSTANCE = Mappers.getMapper(MyStrategyOrderMapper.class);

    @Mapping(target = "id", ignore = true) // Bỏ qua id vì tự sinh
    MyStrategyOrder toEntity(MyStrategyOrderRequest request);

    MyStrategyOrderRequest toDto(MyStrategyOrder entity);
}
