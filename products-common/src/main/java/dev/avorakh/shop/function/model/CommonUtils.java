package dev.avorakh.shop.function.model;

import dev.avorakh.shop.common.utils.response.ErrorDetail;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class CommonUtils {

    public static final String TITLE_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK =
            "the 'title' field should be present or not blank";
    public static final String DESCRIPTION_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK =
            "the 'description' field should be present or not blank";
    public static final String PRICE_FIELD_SHOULD_BE_PRESENT = "the 'price' field should be present";
    public static final String PRICE_FIELD_SHOULD_BE_GREATER_THAN_0 = "the 'price' field should be greater than 0";
    public static final String COUNT_FIELD_SHOULD_BE_PRESENT = "the 'count' field should be present";
    public static final String COUNT_FIELD_SHOULD_BE_EQUAL_TO_OR_GREATER_THAN_0 =
            "the 'count' field should be equal to or greater than 0";

    public List<ErrorDetail> validate(ProductInputResource resource) {
        var errorDetails = new ArrayList<ErrorDetail>(4);

        if (StringUtils.isBlank(resource.getTitle())) {
            errorDetails.add(new ErrorDetail(TITLE_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK));
        }

        if (StringUtils.isBlank(resource.getDescription())) {
            errorDetails.add(new ErrorDetail(DESCRIPTION_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK));
        }

        Integer price = resource.getPrice();
        if (price == null) {
            errorDetails.add(new ErrorDetail(PRICE_FIELD_SHOULD_BE_PRESENT));
        } else if (price <= 0) {
            errorDetails.add(new ErrorDetail(PRICE_FIELD_SHOULD_BE_GREATER_THAN_0));
        }

        Integer count = resource.getCount();
        if (count == null) {
            errorDetails.add(new ErrorDetail(COUNT_FIELD_SHOULD_BE_PRESENT));
        } else if (count <= 0) {
            errorDetails.add(new ErrorDetail(COUNT_FIELD_SHOULD_BE_EQUAL_TO_OR_GREATER_THAN_0));
        }

        return errorDetails;
    }
}
