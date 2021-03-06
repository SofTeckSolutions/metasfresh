DROP FUNCTION IF EXISTS de_metas_endcustomer_fresh_reports.Docs_Sales_Order_Root(IN record_id numeric, IN ad_language Character Varying (6));

CREATE OR REPLACE FUNCTION de_metas_endcustomer_fresh_reports.Docs_Sales_Order_Root(IN record_id numeric, IN ad_language Character Varying (6))
RETURNS TABLE 
	(
	ad_org_id numeric(10,0),
	docstatus character(2),
	printname character varying(60),
	C_Currency_ID numeric,
	poreference varchar(60),
	displayhu text,
	isoffer character(1)
	)
AS
$$	

SELECT
	o.AD_Org_ID,
	o.DocStatus,
	dt.PrintName,
	o.C_Currency_ID,
	poreference,
	CASE
		WHEN
		EXISTS(
			SELECT 0
			FROM C_OrderLine ol
			INNER JOIN M_Product p ON ol.M_Product_ID = p.M_Product_ID AND p.isActive = 'Y'
			INNER JOIN M_Product_Category pc ON p.M_Product_Category_ID = pc.M_Product_Category_ID AND pc.isActive = 'Y'
			WHERE pc.M_Product_Category_ID = getSysConfigAsNumeric('PackingMaterialProductCategoryID', ol.AD_Client_ID, ol.AD_Org_ID)
			AND ol.C_Order_ID = o.C_Order_ID AND ol.isActive = 'Y'
		)
		THEN 'Y'
		ELSE 'N'
	END as displayhu,
		CASE WHEN dt.docbasetype = 'SOO' AND dt.docsubtype IN ('ON', 'OB')
		THEN 'Y'
		ELSE 'N'
	END AS isoffer
FROM
	C_Order o
	INNER JOIN C_DocType dt ON o.C_DocTypeTarget_ID = dt.C_DocType_ID AND dt.isActive = 'Y'
	LEFT OUTER JOIN C_DocType_Trl dtt ON o.C_DocTypeTarget_ID = dtt.C_DocType_ID AND dtt.AD_Language = $2 AND dtt.isActive = 'Y'
WHERE
	o.C_Order_ID = $1 AND o.isActive = 'Y'
$$
LANGUAGE sql STABLE
;


DROP FUNCTION IF EXISTS de_metas_endcustomer_fresh_reports.Docs_Sales_Order_Description(IN record_id numeric, IN ad_language Character Varying (6));

CREATE OR REPLACE FUNCTION de_metas_endcustomer_fresh_reports.Docs_Sales_Order_Description(IN record_id numeric, IN ad_language Character Varying (6))

RETURNS TABLE
(
	description character varying(1024),
	documentno character varying(30),
	dateordered timestamp without time zone,
	reference text,
	isoffer text,
	offervaliddate timestamp without time zone,
	offervaliddays numeric(10,0),
	bp_value character varying(40),
	cont_name text,
	cont_phone character varying(40),
	cont_fax character varying(40),
	cont_email character varying(60),
	sr_name text,
	sr_phone character varying(40),
	sr_fax character varying(40),
	sr_email character varying(60),
	printname character varying(60),
	datepromised timestamp with time zone,
	dt_description text
)
AS
$$
SELECT
	o.description 	as description,
	o.documentno 	as documentno,
	o.dateordered	as dateordered,
	o.poreference	as reference,
	CASE WHEN dt.docbasetype = 'SOO' AND dt.docsubtype IN ('ON', 'OB')
		THEN 'Y'
		ELSE 'N'
	END AS isoffer,
	o.offervaliddate,
	o.offervaliddays,
	bp.value	as bp_value,
	Coalesce(cogrt.name, cogrt.name, '') ||
	Coalesce(' ' || cont.title, '') ||
	Coalesce(' ' || cont.firstName, '') ||
	Coalesce(' ' || cont.lastName, '') as cont_name,
	cont.phone	as cont_phone,
	cont.fax	as cont_fax,
	cont.email	as cont_email,
	Coalesce(srgrt.name, srgr.name, '') ||
	Coalesce(' ' || srep.title, '') ||
	Coalesce(' ' || srep.firstName, '') ||
	Coalesce(' ' || srep.lastName, '') as sr_name,
	srep.phone	as sr_phone,
	srep.fax	as sr_fax,
	srep.email	as sr_email,
	COALESCE(dtt.PrintName, dt.PrintName) AS PrintName,
	o.datepromised,
	COALESCE(dtt.Description, dt.Description) AS dt_description
FROM
	C_Order o
	INNER JOIN C_BPartner bp 		ON o.C_BPartner_ID = bp.C_BPartner_ID AND bp.isActive = 'Y'
	LEFT OUTER JOIN AD_User srep		ON o.SalesRep_ID = srep.AD_User_ID AND srep.AD_User_ID <> 100 AND srep.isActive = 'Y'
	LEFT OUTER JOIN AD_User cont		ON o.Bill_User_ID = cont.AD_User_ID AND cont.isActive = 'Y'
	LEFT OUTER JOIN C_DocType dt 		ON o.C_DocTypeTarget_ID = dt.C_DocType_ID AND dt.isActive = 'Y'
	LEFT OUTER JOIN C_DocType_Trl dtt 	ON o.C_DocTypeTarget_ID = dtt.C_DocType_ID AND dtt.AD_Language = $2 AND dtt.isActive = 'Y'

	-- Translatables
	LEFT OUTER JOIN C_Greeting cogr	ON cont.C_Greeting_ID = cogr.C_Greeting_ID AND cogr.isActive = 'Y'
	LEFT OUTER JOIN C_Greeting_Trl cogrt	ON cont.C_Greeting_ID = cogrt.C_Greeting_ID AND cogrt.ad_language = $2 AND cogrt.isActive = 'Y'
	LEFT OUTER JOIN C_Greeting srgr	ON srep.C_Greeting_ID = srgr.C_Greeting_ID AND srgr.isActive = 'Y'
	LEFT OUTER JOIN C_Greeting_Trl srgrt	ON srep.C_Greeting_ID = srgrt.C_Greeting_ID AND srgrt.ad_language = $2 AND srgrt.isActive = 'Y'
WHERE
	o.C_Order_ID = $1 AND o.isActive = 'Y'
$$
LANGUAGE sql STABLE
;





DROP FUNCTION IF EXISTS de_metas_endcustomer_fresh_reports.Docs_Sales_Order_Details_Footer (IN C_Order_ID numeric, IN AD_Language Character Varying(6));
CREATE OR REPLACE FUNCTION de_metas_endcustomer_fresh_reports.Docs_Sales_Order_Details_Footer(IN C_Order_ID numeric, IN AD_Language Character Varying(6))
    RETURNS TABLE
            (
                paymentrule    character varying(60),
                paymentterm    character varying(60),
                discount1      numeric,
                discount2      numeric,
                discount_date1 text,
                discount_date2 text,
                cursymbol      character varying(10),
                documentnote   text
            )
AS
$$
SELECT COALESCE(reft.name, ref.name)                                                                            AS paymentrule,
       COALESCE(ptt.name, pt.name)                                                                              as paymentterm,
       (CASE
            WHEN pt.DiscountDays > 0 THEN (o.grandtotal + (o.grandtotal * pt.discount / 100))
            ELSE null END)                                                                                      AS discount1,
       (CASE
            WHEN pt.DiscountDays2 > 0 THEN (o.grandtotal + (o.grandtotal * pt.discount2 / 100))
            ELSE null END)                                                                                      AS discount2,
       to_char((o.DateOrdered - DiscountDays), 'dd.MM.YYYY')                                                    AS discount_date1,
       to_char((o.DateOrdered - DiscountDays2), 'dd.MM.YYYY')                                                   AS discount_date2,
       c.cursymbol,
       COALESCE(nullif(dtt.documentnote, ''),
                nullif(dt.documentnote, ''))                                                                    as documentnote
FROM C_Order o

         LEFT OUTER JOIN C_PaymentTerm pt on o.C_PaymentTerm_ID = pt.C_PaymentTerm_ID AND pt.isActive = 'Y'
         LEFT OUTER JOIN C_PaymentTerm_Trl ptt
                         on o.C_PaymentTerm_ID = ptt.C_PaymentTerm_ID AND ptt.AD_Language = $2 AND ptt.isActive = 'Y'

         LEFT OUTER JOIN AD_Ref_List ref ON o.PaymentRule = ref.Value AND ref.AD_Reference_ID = (SELECT AD_Reference_ID
                                                                                                 FROM AD_Reference
                                                                                                 WHERE name = '_Payment Rule'
                                                                                                   AND isActive = 'Y') AND
                                            ref.isActive = 'Y'
         LEFT OUTER JOIN AD_Ref_List_Trl reft
                         ON reft.AD_Ref_List_ID = ref.AD_Ref_List_ID AND reft.AD_Language = $2 AND reft.isActive = 'Y'
         INNER JOIN C_Currency c ON o.C_Currency_ID = c.C_Currency_ID AND c.isActive = 'Y'

-- take out document type notes
         INNER JOIN C_DocType dt ON o.C_DocTypeTarget_ID = dt.C_DocType_ID AND dt.isActive = 'Y'
         LEFT OUTER JOIN C_DocType_Trl dtt
                         ON o.C_DocTypeTarget_ID = dtt.C_DocType_ID AND dtt.AD_Language = $2 AND dtt.isActive = 'Y'

WHERE o.C_Order_ID = $1
  AND o.isActive = 'Y'

$$
    LANGUAGE sql STABLE;
