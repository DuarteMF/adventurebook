package com.demo.adventurebook.story.repository;

import com.demo.adventurebook.story.entity.StoryOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryOptionRepository extends JpaRepository<StoryOption, Long> {

    List<StoryOption> findByStorySectionId(Long sectionId);
}
