package com.demo.adventurebook.story.repository;

import com.demo.adventurebook.story.entity.Difficulty;
import com.demo.adventurebook.story.entity.Story;
import org.springframework.data.jpa.domain.Specification;

public final class StorySpecifications {

    private StorySpecifications() {
    }

    /**
     * Case-insensitive substring match against title OR author.
     */
    public static Specification<Story> titleOrAuthorContains(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern)
            );
        };
    }

    public static Specification<Story> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Story> isValid() {
        return (root, query, cb) -> cb.isTrue(root.get("valid"));
    }
}
