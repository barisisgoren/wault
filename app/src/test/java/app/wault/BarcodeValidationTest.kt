package app.wault

import app.wault.data.CodeFormat
import app.wault.ui.code.canEncode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeValidationTest {
    @Test fun ean13WithCorrectCheckDigit() = assertTrue(canEncode("8690000012343", CodeFormat.EAN_13))
    @Test fun ean13WithWrongCheckDigitIsRejected() = assertFalse(canEncode("8690000012345", CodeFormat.EAN_13))
    @Test fun ean13WrongLengthIsRejected() = assertFalse(canEncode("86900000123", CodeFormat.EAN_13))
    @Test fun anyTextWorksAsQr() = assertTrue(canEncode("GYM-MEMBER-22841", CodeFormat.QR))
    @Test fun code128AcceptsAscii() = assertTrue(canEncode("A-1234", CodeFormat.CODE_128))
    @Test fun blankIsNotEncodable() = assertFalse(canEncode("  ", CodeFormat.QR))
}
