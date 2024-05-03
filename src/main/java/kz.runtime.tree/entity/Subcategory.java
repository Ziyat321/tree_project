package kz.runtime.tree.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tree")
public class Subcategory {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "left_key")
    private Long left_key;

    @Column(name = "right_key")
    private Long right_key;

    @Column(name = "level")
    private Long level;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLeft_key() {
        return left_key;
    }

    public void setLeft_key(Long left_key) {
        this.left_key = left_key;
    }

    public Long getRight_key() {
        return right_key;
    }

    public void setRight_key(Long right_key) {
        this.right_key = right_key;
    }

    public Long getLevel() {
        return level;
    }

    public void setLevel(Long level) {
        this.level = level;
    }
}
