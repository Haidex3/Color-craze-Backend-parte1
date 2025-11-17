package com.colorcraze.board.models;

import com.colorcraze.utils.enums.ColorStatus;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Player.class, name = "Player"),
    @JsonSubTypes.Type(value = Platform.class, name = "Platform")
})
@Getter
@Setter
@AllArgsConstructor
public class Box {
    protected ColorStatus color;
}
