package ru.yandex.practicum.shop.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table("users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    private Long id;

    @Column("login")
    private String login;

    @Column("password")
    private String password;

    @Column("authorities")
    private String authorities;
}