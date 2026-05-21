import markdown
from xhtml2pdf import pisa

def convert_md_to_pdf(md_file, pdf_file):
    with open(md_file, 'r', encoding='utf-8') as f:
        md_text = f.read()
    
    html = markdown.markdown(md_text, extensions=['tables', 'fenced_code'])
    
    styled_html = f"""
    <html>
    <head>
    <style>
        @page {{
            size: A4;
            margin: 2cm;
        }}
        body {{
            font-family: Helvetica, Arial, sans-serif;
            font-size: 11pt;
            line-height: 1.5;
            color: #333333;
        }}
        h1 {{
            color: #e63946;
            text-align: center;
            border-bottom: 2px solid #e63946;
            padding-bottom: 10px;
        }}
        h2 {{
            color: #1d3557;
            margin-top: 25px;
            border-bottom: 1px solid #ccc;
            padding-bottom: 5px;
        }}
        h3 {{
            color: #457b9d;
            margin-top: 20px;
        }}
        ul, ol {{
            margin-bottom: 15px;
            margin-left: 20px;
        }}
        li {{
            margin-bottom: 5px;
        }}
        code {{
            background-color: #f1f1f1;
            padding: 2px 5px;
            border-radius: 3px;
            font-family: Courier, monospace;
        }}
        .highlight {{
            background-color: #f1f1f1;
            padding: 10px;
            border-left: 4px solid #e63946;
            margin: 15px 0;
        }}
    </style>
    </head>
    <body>
    {html}
    </body>
    </html>
    """
    
    with open(pdf_file, "w+b") as result_file:
        pisa_status = pisa.CreatePDF(styled_html, dest=result_file)
        
    if pisa_status.err:
        print("Error generating PDF")
    else:
        print("PDF generated successfully:", pdf_file)

if __name__ == "__main__":
    convert_md_to_pdf("docs/Nigehban_AI_Documentation.md", "docs/Nigehban_AI_Documentation.pdf")
