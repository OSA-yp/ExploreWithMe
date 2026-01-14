package ru.practicum.explore.server.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class NewUserRequestDto {

    @Length(min = 2, max = 250)
    @NotBlank
    private String name;

    @Length(min = 6, max = 254)
    @NotBlank
    @Email
    private String email;

}
