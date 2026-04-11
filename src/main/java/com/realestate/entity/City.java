package com.realestate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @JsonIgnore
    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY)
    private List<Locality> localities;
}
