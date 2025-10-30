package com.example.healthai.drug.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.drug.domain.Medicine;

@Mapper
public interface MedicineMapper {

    Optional<Medicine> findById(@Param("id") Long id);

    Optional<Medicine> findByGenericName(@Param("genericName") String genericName);

    List<Medicine> search(@Param("keyword") String keyword,
                          @Param("contraindication") String contraindication,
                          @Param("limit") Integer limit,
                          @Param("offset") Integer offset);

    long count(@Param("keyword") String keyword,
               @Param("contraindication") String contraindication);

    int insert(Medicine medicine);

    int update(Medicine medicine);

    int delete(@Param("id") Long id);
}
