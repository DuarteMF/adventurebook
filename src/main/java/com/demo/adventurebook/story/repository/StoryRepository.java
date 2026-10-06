package com.demo.adventurebook.story.repository;

import com.demo.adventurebook.story.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, Long>, JpaSpecificationExecutor<Story> {

    Optional<Story> findBySourceFilename(String sourceFilename);

    boolean existsBySourceFilename(String sourceFilename);
}
