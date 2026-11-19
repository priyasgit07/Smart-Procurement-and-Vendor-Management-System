/**
 * Smart Procurement & Vendor Management System
 * Currency Formatter Utility for Indian Rupees (INR - ₹)
 * Uses Intl.NumberFormat with locale 'en-IN' and currency 'INR'.
 */
const inrCurrencyFormatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

/**
 * Formats a given numeric amount into Indian Rupee (INR) currency format.
 * Example: 110000 -> "₹1,10,000.00"
 *
 * @param {number|string} amount
 * @returns {string} Formatted INR currency string
 */
function formatINR(amount) {
    if (amount === null || amount === undefined || isNaN(Number(amount))) {
        return inrCurrencyFormatter.format(0);
    }
    return inrCurrencyFormatter.format(Number(amount));
}
