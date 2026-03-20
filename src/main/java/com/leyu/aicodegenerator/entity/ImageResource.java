package com.leyu.aicodegenerator.entity;

import com.leyu.aicodegenerator.model.enums.ImageCategoryEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Image resource object.
 */
/** ImageResource implementation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResource implements Serializable {

    /**
     * Image category.
     */
    private ImageCategoryEnum category;

    /**
     * Image description.
     */
    private String description;

    /**
     * Image URL.
     */
    private String url;

    @Serial
    private static final long serialVersionUID = 1L;
}
