The Technical University of Munich (TUM) takes the protection of personal data very seriously and uses secure and encrypted communication according to best practices and state-of-the-art technologies (e.g. HTTPS with a secure certificate, TLS 1.3, Strict Transport Security, Forward Secrecy, Same-Site cookie protection) to protect the privacy of LEARN-Hub users in the best possible way. LEARN-Hub is a research prototype developed at the Chair of Applied Education Technologies and processes personal data in the context of teaching and educational research in compliance with the applicable data protection regulations. Unless otherwise stated, the legal basis for processing the data is Art. 6(1)(e) GDPR in conjunction with Art. 4(1) BayDSG (performance of a task carried out in the public interest). In addition, Art. 84 Bayerisches Hochschulinnovationsgesetz (BayHIG) applies.

In the following, we provide information on the type, scope and purpose of the collection and use of personal data. This information can be accessed at any time from this page.

## General Information

### Name and contact details of the controller

Prof. Dr. Stephan Krusche
krusche@tum.de
+49 89 289 18233

Technical University of Munich
Postal address: Arcisstraße 21, 80333 Munich
Telephone: +49-(0)89-289-01
Email: poststelle@tum.de

### Contact details of the data protection officer

The data protection officer of the Technical University of Munich
Postal address: Arcisstraße 21, 80333 Munich
Telephone: 089/289-17052
Email: beauftragter@datenschutz.tum.de

### Purposes and legal bases for the processing of personal data

The purpose of the processing is to fulfil the public duties assigned to us by the legislator, in particular teaching, lesson preparation and educational research in the university environment. LEARN-Hub supports teachers in finding and preparing computer-science teaching activities. Unless otherwise stated, the legal basis for processing your data results from Art. 6(1)(e) GDPR in conjunction with Art. 4(1) BayDSG (performance of a task carried out in the public interest). In addition, Art. 84 Bayerisches Hochschulinnovationsgesetz (BayHIG) applies. Accordingly, we are permitted to process the data required to fulfil a duty incumbent upon us.

### Recipients of personal data

LEARN-Hub is operated on the IT infrastructure of the Technical University of Munich (Chair of Applied Education Technologies, TUM School of Computation, Information and Technology). Personal data you transmit while using the platform is processed on this infrastructure.

For two clearly delimited functions, content is transmitted to external processors within the scope of a data processing agreement:

- AI-assisted content processing: the content required for the respective request is transmitted to Microsoft Azure OpenAI Service (see "AI-assisted content processing" below).
- PDF-to-DOCX conversion: document files selected for editable download are transmitted to Adobe PDF Services (see "PDF-to-DOCX conversion" below).

### Duration of the storage of personal data

Your data will only be stored for as long as is necessary for the fulfilment of the purposes described here. Account data is stored for the duration of use of the platform and is deleted upon deletion of your account or on request, unless statutory retention periods prevent deletion. Specific retention periods for the individual processing operations can be found in the respective sections below.

### Your rights

Insofar as we process personal data from you, you are entitled to the following rights as a data subject:

- You have the right of access to the data stored about you (Art. 15 GDPR).
- If incorrect personal data is processed, you have the right to rectification (Art. 16 GDPR).
- If the legal requirements are met, you may request the deletion or restriction of processing (Art. 17 and 18 GDPR).
- If you have consented to the processing or if there is a contract for data processing and the data processing is carried out with the help of automated procedures, you may have a right to data portability (Art. 20 GDPR).
- If you have consented to the processing and the processing is based on this consent, you can revoke the consent at any time for the future. The lawfulness of the data processing carried out on the basis of the consent until the revocation is not affected by this.
- You have the right to object to the processing of your data at any time on grounds relating to your particular situation, if the processing is carried out on the basis of Art. 6(1)(e) GDPR (Art. 21(1)(1) GDPR).

### Right to lodge a complaint with the supervisory authority

Furthermore, you have the right to lodge a complaint with the Bavarian State Commissioner for Data Protection. You can reach them under the following contact details:

Postal address: Postfach 22 12 19, 80502 Munich
Address: Wagmüllerstraße 18, 80538 Munich
Telephone: 089 212672-0
Email: poststelle@datenschutz-bayern.de
https://www.datenschutz-bayern.de/

## Information about the web presence

### Technical implementation

The web servers of LEARN-Hub are operated on the IT infrastructure of the Technical University of Munich. The personal data you transmit when visiting our web presence is processed there.

### Logging

When you access LEARN-Hub, you transmit data to our web servers via your Internet browser. The following data is temporarily recorded in a log file during an ongoing connection between your Internet browser and our web servers:

- IP address of the requesting computer
- Date and time of access
- Name, URL and transferred data volume of the retrieved file
- Access status (requested file transferred, not found, etc.)
- Identification data of the browser and operating system used (if transmitted by the requesting web browser)
- Web page from which access was made (if transmitted by the requesting web browser)

The log entries may be evaluated in order to detect and react to attacks on the web servers and, in the event of reported malfunctions, errors and security incidents, to carry out a manual analysis.

Retention period: Log files are automatically deleted after 90 days, unless they are needed to investigate a specific security incident.

### Cookies

In order to provide the functions of LEARN-Hub, we use so-called "cookies". You can deactivate the storage of cookies or set your browser so that cookies are only stored for the duration of the respective connection. This could, however, limit the functional scope of our offering. LEARN-Hub uses exclusively technically necessary cookies:

- Session and CSRF cookie: to maintain your login session (Spring Security) and to protect requests against cross-site request forgery. The session cookie is deleted after logout, after the browser is closed or when the configured validity period expires.
- Language setting: to store your preferred language (German/English).
- Theme setting: to store your preferred appearance (light/dark).

No cookies are used for tracking or advertising purposes. Technically necessary cookies do not require consent pursuant to § 25(2) no. 2 TDDDG.

## Information on individual processing operations

### Registration and login

To use the functions reserved for teachers and administrators, you create an account. Authentication is performed either with email address and password or with a one-time verification code sent to your email address. The following data is processed:

- First name and last name
- Email address
- Password (stored exclusively in salted and hashed form; not stored in plain text)
- One-time email verification codes (short-lived)
- Assigned role (guest, teacher, administrator)

The connection is encrypted (HTTPS). Guests can browse public content without an account. Registration and login data is processed to secure access and to attribute your search history and favourites to your account.

Retention period: Account data is stored for the duration of use of the platform. Upon deletion of your account or on request, this data is deleted, unless statutory retention periods prevent deletion.

### Usage data in the context of teaching

As part of the use of LEARN-Hub, the following data required for the provision of the service is processed:

- Search and recommendation requests (e.g. target age group, available resources, learning objectives) and your search history
- Favourites and saved lesson plans
- Educational activity documents (e.g. PDFs) that teachers or administrators upload for processing and their extracted metadata

Retention period: Search history and favourites are stored for the duration of use of the platform and are deleted upon deletion of your account or on request. You can delete individual entries of your search history at any time in the platform.

### AI-assisted content processing (Microsoft Azure OpenAI)

LEARN-Hub uses a large language model to support content processing – in particular to extract pedagogical metadata from uploaded activity documents and to generate teaching materials (e.g. exercise sheets, and optionally illustrative images). The model used is GPT-4.1, provided via the Microsoft Azure OpenAI Service in a data centre within the European Union.

Which data is processed: the content required for the respective request is transmitted to Azure for processing. This is primarily the content of the educational activity documents and the parameters of your request. These documents are pedagogical materials and generally do not contain personal data of platform users; your matriculation number, email address and password are never transmitted.

Data security:

- Processing takes place in EU data centres of Microsoft Azure on the basis of a data processing agreement (Auftragsverarbeitungsvertrag, AVV) between TUM and Microsoft.
- Your data is not used to train or improve AI models.
- All data transfers are encrypted.
- The content is transmitted to Azure only for processing the respective request and is not stored permanently there.

Legal basis: Art. 6(1)(e) GDPR in conjunction with Art. 4(1) BayDSG. The AI-assisted processing is part of the platform's task of supporting teaching and lesson preparation; it operates on document content rather than on the personal data of users.

### PDF-to-DOCX conversion (Adobe PDF Services)

So that teaching materials can be downloaded and further edited in an editable format, LEARN-Hub offers an optional conversion of PDF documents into the Word format (DOCX). This conversion is carried out via Adobe PDF Services, a cloud service of Adobe.

Which data is processed: only when you actively request an editable (DOCX) download, the relevant document file is transmitted to Adobe PDF Services for conversion and the converted file is returned. The transmitted files are the educational activity documents; no account data (name, email address, password) is transmitted for this purpose. Adobe processes the files solely to carry out the conversion. The transmitted document and the converted DOCX are temporarily stored by Adobe to perform the conversion and are automatically deleted within 24 hours. To avoid redundant transmissions, LEARN-Hub caches the converted DOCX files on the TUM infrastructure.

The conversion is carried out in Adobe's European (EMEA) data centres. The transmission takes place solely for the purpose of the requested conversion and only for documents that you yourself select for editable download.

Legal basis: Art. 6(1)(e) GDPR in conjunction with Art. 4(1) BayDSG.

### Email notifications

LEARN-Hub sends emails to the address you provided, in particular one-time verification codes for login and account-related messages (e.g. password reset). The email address is used exclusively for communication in the context of platform operation.

Retention period: The retention period for the email address is governed by the section "Registration and login".

### Automated decision-making

LEARN-Hub generates activity recommendations using a transparent, category-based scoring algorithm (e.g. age appropriateness, topic relevance, duration fit, alignment with learning objectives). Teachers receive a detailed breakdown of how a recommendation was generated. These recommendations are decision support for teachers and do not produce legal effects or similarly significant effects for you. No profiling within the meaning of Art. 22 GDPR takes place.

## Disclosure and rectification

You have the right, upon written request and free of charge, to obtain information about the personal data stored about you. In addition, you have the right to have incorrect data corrected. You can reach the data protection officer of the Technical University of Munich by email at beauftragter@datenschutz.tum.de or via [www.datenschutz.tum.de](https://www.datenschutz.tum.de).
