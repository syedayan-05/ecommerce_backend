
            package com.ayan.ecommerce.service;

    import jakarta.mail.MessagingException;
    import jakarta.mail.internet.MimeMessage;
    import lombok.RequiredArgsConstructor;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.core.io.ClassPathResource;
    import org.springframework.mail.javamail.JavaMailSender;
    import org.springframework.mail.javamail.MimeMessageHelper;
    import org.springframework.stereotype.Service;

    import java.io.UnsupportedEncodingException;
    import java.nio.charset.StandardCharsets;

    @Service
    @RequiredArgsConstructor
    public class EmailService {

        private final JavaMailSender mailSender;

        @Value("${app.mail.from}")
        private String fromEmail;

        @Value("${app.mail.from-name}")
        private String fromName;


        // =========================================================
        // COMMON HTML EMAIL SENDER
        // =========================================================

        private void sendHtmlEmail(
                String toEmail,
                String subject,
                String htmlContent
        ) {

            try {

                MimeMessage message =
                        mailSender.createMimeMessage();

                MimeMessageHelper helper =
                        new MimeMessageHelper(
                                message,
                                true,
                                StandardCharsets.UTF_8.name()
                        );

                // -------------------------------------------------
                // SENDER
                // -------------------------------------------------

                helper.setFrom(
                        fromEmail,
                        fromName
                );

                // -------------------------------------------------
                // RECEIVER
                // -------------------------------------------------

                helper.setTo(toEmail);

                // -------------------------------------------------
                // SUBJECT
                // -------------------------------------------------

                helper.setSubject(subject);

                // -------------------------------------------------
                // HTML CONTENT
                // -------------------------------------------------

                helper.setText(
                        htmlContent,
                        true
                );

                // -------------------------------------------------
                // INLINE SUFI LEATHER LOGO
                // -------------------------------------------------

                ClassPathResource logo =
                        new ClassPathResource(
                                "email/images/sufi-leather-logo.jpg"
                        );

                if (logo.exists()) {

                    helper.addInline(
                            "sufiLogo",
                            logo,
                            "image/jpeg"
                    );
                }

                // -------------------------------------------------
                // SEND
                // -------------------------------------------------

                mailSender.send(message);

            } catch (MessagingException e) {

                throw new RuntimeException(
                        "Failed to send email to " + toEmail,
                        e
                );
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }


        // =========================================================
        // EMAIL VERIFICATION OTP
        // =========================================================

        public void sendVerificationOtp(
                String toEmail,
                String otp
        ) {

            String content = """
                    <p>
                        Welcome to <strong>Sufi Leather</strong>.
                        We're excited to have you with us.
                    </p>
    
                    <p>
                        Please use the verification code below
                        to verify your email address.
                    </p>
    
                    <div style="
                        margin:32px 0;
                        text-align:center;
                    ">
    
                        <div style="
                            display:inline-block;
                            padding:18px 32px;
                            background:#f6efe5;
                            border:1px solid #dfc9aa;
                            border-radius:10px;
                            font-size:32px;
                            line-height:1;
                            letter-spacing:8px;
                            font-weight:700;
                            color:#3d260b;
                        ">
                            %OTP%
                        </div>
    
                    </div>
    
                    <p style="
                        color:#777777;
                        font-size:14px;
                        line-height:1.6;
                    ">
                        This verification code is valid for
                        <strong>5 minutes</strong>.
                    </p>
    
                    <p style="
                        color:#777777;
                        font-size:14px;
                        line-height:1.6;
                    ">
                        If you did not create a Sufi Leather account,
                        you can safely ignore this email.
                    </p>
                    """
                    .replace(
                            "%OTP%",
                            escapeHtml(otp)
                    );

            String html =
                    baseTemplate(
                            "Verify Your Email",
                            content
                    );

            sendHtmlEmail(
                    toEmail,
                    "Verify Your Sufi Leather Account",
                    html
            );
        }


        // =========================================================
        // PASSWORD RESET OTP
        // =========================================================

        public void sendPasswordResetOtp(
                String toEmail,
                String otp
        ) {

            String content = """
                    <p>
                        We received a request to reset your
                        <strong>Sufi Leather</strong> account password.
                    </p>
    
                    <p>
                        Use the secure verification code below
                        to continue.
                    </p>
    
                    <div style="
                        margin:32px 0;
                        text-align:center;
                    ">
    
                        <div style="
                            display:inline-block;
                            padding:18px 32px;
                            background:#f6efe5;
                            border:1px solid #dfc9aa;
                            border-radius:10px;
                            font-size:32px;
                            line-height:1;
                            letter-spacing:8px;
                            font-weight:700;
                            color:#3d260b;
                        ">
                            %OTP%
                        </div>
    
                    </div>
    
                    <p style="
                        color:#777777;
                        font-size:14px;
                        line-height:1.6;
                    ">
                        This password reset code will expire in
                        <strong>5 minutes</strong>.
                    </p>
    
                    <p style="
                        color:#777777;
                        font-size:14px;
                        line-height:1.6;
                    ">
                        If you did not request a password reset,
                        please ignore this email.
                    </p>
                    """
                    .replace(
                            "%OTP%",
                            escapeHtml(otp)
                    );

            String html =
                    baseTemplate(
                            "Reset Your Password",
                            content
                    );

            sendHtmlEmail(
                    toEmail,
                    "Reset Your Sufi Leather Password",
                    html
            );
        }


        // =========================================================
        // WELCOME EMAIL
        // =========================================================

        public void sendWelcomeEmail(
                String toEmail,
                String customerName
        ) {

            String content = """
                    <p style="
                        font-size:18px;
                        margin-top:0;
                    ">
                        Hello <strong>%CUSTOMER_NAME%</strong>,
                    </p>
    
                    <p>
                        Welcome to <strong>Sufi Leather</strong>.
                        Your account has been successfully created.
                    </p>
    
                    <div style="
                        margin:30px 0;
                        padding:22px 24px;
                        background:#f8f4ee;
                        border-left:4px solid #c99a4a;
                        border-radius:6px;
                    ">
    
                        <p style="
                            margin:0;
                            color:#3d260b;
                            font-size:16px;
                            line-height:1.6;
                            font-weight:600;
                        ">
                            Crafted with tradition.
                            Designed for you.
                        </p>
    
                    </div>
    
                    <p>
                        You can now explore our collection of
                        handcrafted leather products.
                    </p>
    
                    <p>
                        Thank you for choosing
                        <strong>Sufi Leather</strong>.
                    </p>
                    """
                    .replace(
                            "%CUSTOMER_NAME%",
                            escapeHtml(customerName)
                    );

            String html =
                    baseTemplate(
                            "Welcome to Sufi Leather",
                            content
                    );

            sendHtmlEmail(
                    toEmail,
                    "Welcome to Sufi Leather",
                    html
            );
        }


        // =========================================================
        // ORDER CONFIRMATION
        // =========================================================

        public void sendOrderConfirmation(
                String toEmail,
                String customerName,
                String orderNumber,
                Double amount
        ) {

            String formattedAmount =
                    String.format(
                            "₹%.2f",
                            amount
                    );

            String content = """
                    <p style="
                        font-size:18px;
                        margin-top:0;
                    ">
                        Hello <strong>%CUSTOMER_NAME%</strong>,
                    </p>
    
                    <p>
                        Thank you for shopping with
                        <strong>Sufi Leather</strong>.
                    </p>
    
                    <p>
                        Your order has been successfully placed
                        and is now being processed.
                    </p>
    
                    <!-- ORDER DETAILS -->
    
                    <div style="
                        margin:30px 0;
                        border:1px solid #eadfce;
                        border-radius:10px;
                        overflow:hidden;
                    ">
    
                        <table width="100%"
                               cellpadding="0"
                               cellspacing="0"
                               border="0"
                               style="
                                   border-collapse:collapse;
                                   font-size:15px;
                               ">
    
                            <tr>
    
                                <td style="
                                    padding:16px;
                                    background:#f8f4ee;
                                    color:#666666;
                                ">
                                    Order Number
                                </td>
    
                                <td style="
                                    padding:16px;
                                    background:#f8f4ee;
                                    text-align:right;
                                    font-weight:700;
                                    color:#3d260b;
                                ">
                                    %ORDER_NUMBER%
                                </td>
    
                            </tr>
    
                            <tr>
    
                                <td style="
                                    padding:16px;
                                    color:#666666;
                                    border-top:1px solid #eadfce;
                                ">
                                    Total Amount
                                </td>
    
                                <td style="
                                    padding:16px;
                                    text-align:right;
                                    font-size:18px;
                                    font-weight:700;
                                    color:#3d260b;
                                    border-top:1px solid #eadfce;
                                ">
                                    %AMOUNT%
                                </td>
    
                            </tr>
    
                        </table>
    
                    </div>
    
    
                    <p>
                        We'll keep you updated as your order
                        moves through the delivery process.
                    </p>
    
                    <p>
                        Thank you for choosing
                        <strong>Sufi Leather</strong>.
                    </p>
                    """
                    .replace(
                            "%CUSTOMER_NAME%",
                            escapeHtml(customerName)
                    )
                    .replace(
                            "%ORDER_NUMBER%",
                            escapeHtml(orderNumber)
                    )
                    .replace(
                            "%AMOUNT%",
                            escapeHtml(formattedAmount)
                    );

            String html =
                    baseTemplate(
                            "Order Confirmed",
                            content
                    );

            sendHtmlEmail(
                    toEmail,
                    "Order Confirmed - " + orderNumber,
                    html
            );
        }


        // =========================================================
        // TEST EMAIL
        // =========================================================

        public void sendTestEmail(
                String toEmail
        ) {

            String content = """
                    <p>
                        Hello,
                    </p>
    
                    <p>
                        This is a test email from
                        <strong>Sufi Leather</strong>.
                    </p>
    
                    <div style="
                        margin:30px 0;
                        padding:20px 22px;
                        background:#f3f8f3;
                        border-left:4px solid #4c8c4a;
                        border-radius:6px;
                    ">
    
                        <p style="
                            margin:0;
                            color:#356434;
                            font-weight:600;
                            font-size:15px;
                        ">
                            ✓ Email delivery is working successfully.
                        </p>
    
                    </div>
    
                    <p>
                        Your Sufi Leather email system is ready
                        to send transactional emails.
                    </p>
    
                    <p style="
                        margin-top:30px;
                        color:#777777;
                        font-size:13px;
                    ">
                        This message was generated automatically
                        by the Sufi Leather backend.
                    </p>
                    """;

            String html =
                    baseTemplate(
                            "Email System Ready",
                            content
                    );

            sendHtmlEmail(
                    toEmail,
                    "Sufi Leather - Email System Test",
                    html
            );
        }


        // =========================================================
        // COMMON BASE EMAIL TEMPLATE
        // =========================================================

        private String baseTemplate(
                String title,
                String content
        ) {

            return """
                    <!DOCTYPE html>
    
                    <html>
    
                    <head>
    
                        <meta charset="UTF-8">
    
                        <meta name="viewport"
                              content="width=device-width, initial-scale=1.0">
    
                        <title>%TITLE%</title>
    
                    </head>
    
    
                    <body style="
                        margin:0;
                        padding:0;
                        background:#f3efe9;
                        font-family:Arial, Helvetica, sans-serif;
                        color:#333333;
                    ">
    
                    <!-- OUTER CONTAINER -->
    
                    <table width="100%"
                           cellpadding="0"
                           cellspacing="0"
                           border="0"
                           style="
                               background:#f3efe9;
                               padding:35px 15px;
                           ">
    
                        <tr>
    
                            <td align="center">
    
    
                                <!-- EMAIL CARD -->
    
                                <table width="600"
                                       cellpadding="0"
                                       cellspacing="0"
                                       border="0"
                                       style="
                                           max-width:600px;
                                           width:100%;
                                           background:#ffffff;
                                           border-radius:14px;
                                           overflow:hidden;
                                       ">
    
    
                                    <!-- ========================= -->
                                    <!-- HEADER                      -->
                                    <!-- ========================= -->
    
                                    <tr>
    
                                        <td align="center"
                                            style="
                                                background:#3d260b;
                                                padding:30px 20px;
                                            ">
    
                                            <img
                                                src="cid:sufiLogo"
                                                alt="Sufi Leather"
                                                width="150"
                                                style="
                                                    display:block;
                                                    width:150px;
                                                    max-width:150px;
                                                    height:auto;
                                                    margin:0 auto;
                                                    border:0;
                                                "
                                            >
    
                                        </td>
    
                                    </tr>
    
    
                                    <!-- ========================= -->
                                    <!-- GOLD ACCENT                 -->
                                    <!-- ========================= -->
    
                                    <tr>
    
                                        <td style="
                                            height:4px;
                                            background:#c99a4a;
                                            font-size:0;
                                            line-height:0;
                                        ">
                                        </td>
    
                                    </tr>
    
    
                                    <!-- ========================= -->
                                    <!-- CONTENT                     -->
                                    <!-- ========================= -->
    
                                    <tr>
    
                                        <td style="
                                            padding:40px 35px;
                                            color:#333333;
                                            font-size:16px;
                                            line-height:1.7;
                                        ">
    
                                            <h1 style="
                                                margin:0 0 25px;
                                                text-align:center;
                                                color:#3d260b;
                                                font-size:27px;
                                                line-height:1.3;
                                                font-weight:700;
                                            ">
                                                %TITLE%
                                            </h1>
    
                                            %CONTENT%
    
                                        </td>
    
                                    </tr>
    
    
                                    <!-- ========================= -->
                                    <!-- FOOTER                     -->
                                    <!-- ========================= -->
    
                                    <tr>
    
                                        <td align="center"
                                            style="
                                                background:#3d260b;
                                                padding:25px 20px;
                                            ">
    
                                            <p style="
                                                margin:0 0 8px;
                                                color:#ffffff;
                                                font-size:15px;
                                                font-weight:700;
                                                letter-spacing:2px;
                                            ">
                                                SUFI LEATHER
                                            </p>
    
                                            <p style="
                                                margin:0;
                                                color:#d8c3a5;
                                                font-size:11px;
                                                letter-spacing:2px;
                                            ">
                                                HANDCRAFTED LEATHER
                                            </p>
    
                                            <p style="
                                                margin:15px 0 0;
                                                color:#bca98d;
                                                font-size:11px;
                                            ">
                                                This is an automated email.
                                                Please do not reply.
                                            </p>
    
                                        </td>
    
                                    </tr>
    
    
                                </table>
    
                                <!-- END EMAIL CARD -->
    
                            </td>
    
                        </tr>
    
                    </table>
    
                    <!-- END OUTER CONTAINER -->
    
                    </body>
    
                    </html>
                    """
                    .replace(
                            "%TITLE%",
                            escapeHtml(title)
                    )
                    .replace(
                            "%CONTENT%",
                            content
                    );
        }


        // =========================================================
        // HTML ESCAPING
        // =========================================================

        private String escapeHtml(
                String value
        ) {

            if (value == null) {
                return "";
            }

            return value
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }

