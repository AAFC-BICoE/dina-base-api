package ca.gc.aafc.dina.mapper;

import java.util.Set;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Person;

@Mapper
public interface PersonMapper extends  DinaMapperV2<PersonDTO, Person> {

  PersonMapper INSTANCE = Mappers.getMapper( PersonMapper.class );

 // @Mapping(source = "agent", target = "agent", qualifiedByName = "uuidToPersonExternalRelation")
  PersonDTO toDto(Person entity, @Context Set<String> provided);

  default ComplexObject map(String value) {
    return null;
  }
  default String map(ComplexObject value) {
    return null;
  }
}
