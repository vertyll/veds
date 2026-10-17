<#-- The frame of every e-mail, drawn as the app draws its cards: light by default, and dark where the mail client
     follows a dark system. Mail clients read neither CSS
     variables nor light-dark(), so the app's colors are written here as hex. -->
<#macro emailLayout>
<!DOCTYPE html>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light dark">
    <meta name="supported-color-schemes" content="light dark">
    <style>
        .text a { color: #111827; text-decoration: underline; }
        @media (prefers-color-scheme: dark) {
            .page { background: #111318 !important; }
            .card { background: #1d2024 !important; border-color: #373a40 !important; }
            .text, .text a { color: #e2e2e6 !important; }
        }
    </style>
</head>
<body class="page" style="margin: 0; padding: 0; background: #f9fafb;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" class="page" style="background: #f9fafb;">
    <tr>
        <td align="center" style="padding: 32px 16px;">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width: 560px;">
                <tr>
                    <td class="text" style="padding: 0 4px 16px; font-family: -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 20px; font-weight: 700; color: #111827;">${realmName}</td>
                </tr>
                <tr>
                    <td class="card text" style="background: #ffffff; border: 1px solid #e5e7eb; border-radius: 16px; padding: 24px; font-family: -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 14px; line-height: 1.6; color: #111827;">
                        <#nested>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
</#macro>
