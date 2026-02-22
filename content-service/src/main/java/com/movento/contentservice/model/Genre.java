package com.movento.contentservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = "contents")
@Entity
@Table(name = "genres")
public class Genre extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToMany(mappedBy = "genres", fetch = FetchType.LAZY)
    private Set<Content> contents = new HashSet<>();
    
    // Helper methods
    public void addContent(Content content) {
        this.contents.add(content);
        content.getGenres().add(this);
    }
    
    public void removeContent(Content content) {
        this.contents.remove(content);
        content.getGenres().remove(this);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Genre)) return false;
        return getId() != null && getId().equals(((Genre) o).getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
