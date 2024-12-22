package com.project.HowManyDaysUntilBot.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends CrudRepository<Holiday,Integer> {
    @Query(value="select * from holiday_table where is_state = 1",nativeQuery = true)
    public List<Holiday> findByState();

    @Query(value="select * from holiday_table where month = :desiredMonth",nativeQuery = true)
    public List<Holiday> findByMonth(@Param("desiredMonth")int desiredMonth);

    @Query(value="select * from holiday_table where lower(holiday_name) = :holiday",nativeQuery = true)
    public Optional<Holiday> findByHoliday(@Param("holiday")String holiday);

    @Query(value="select holiday_name from holiday_table ",nativeQuery = true)
    public List<String> getAllHolidays();

    @Query(value="select * from holiday_table where lower(holiday_name) like concat('%',:word,'%')",nativeQuery = true)
    public List<Holiday> findHolidayByWord(@Param("word")String word);

    @Query(value="select * from holiday_table ",nativeQuery = true)
    public List<Holiday> getAllHolidayObjects();
}
