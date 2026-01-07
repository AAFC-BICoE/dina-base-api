package ca.gc.aafc.dina.mapper;

import java.util.Set;

import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.ItemDto;
import ca.gc.aafc.dina.entity.Item;

@Mapper
public interface ItemMapper extends DinaMapperV2<ItemDto, Item> {

  ItemMapper INSTANCE = Mappers.getMapper(ItemMapper.class);

  ItemDto toDto(Item entity, @Context Set<String> provided, @Context String scope);
  Item toEntity(ItemDto dto, @Context Set<String> provided, @Context String scope);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void patchEntity(@MappingTarget Item entity, ItemDto dto, @Context Set<String> provided, @Context String scope);
}
