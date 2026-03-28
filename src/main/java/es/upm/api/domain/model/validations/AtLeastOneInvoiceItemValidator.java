package es.upm.api.domain.model.validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Collection;

public class AtLeastOneInvoiceItemValidator implements ConstraintValidator<AtLeastOneInvoiceItem, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);
        Collection<?> expenses = this.getCollection(beanWrapper, "expenses", "expenseIds");
        Collection<?> incomes = this.getCollection(beanWrapper, "incomes", "incomeIds");

        return !this.isNullOrEmpty(expenses) || !this.isNullOrEmpty(incomes);
    }

    private Collection<?> getCollection(BeanWrapperImpl beanWrapper, String primaryProperty, String alternativeProperty) {
        if (beanWrapper.isReadableProperty(primaryProperty)) {
            return (Collection<?>) beanWrapper.getPropertyValue(primaryProperty);
        }
        if (beanWrapper.isReadableProperty(alternativeProperty)) {
            return (Collection<?>) beanWrapper.getPropertyValue(alternativeProperty);
        }
        return null;
    }

    private boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
