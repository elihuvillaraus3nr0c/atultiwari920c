<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Html View in PDF</title>
<style>
	.align-center{
		text-align: center;
	}
	.hidden{
		display:none;
	}
</style>
</head>
<body>
<input type="button" onclick="withFile()" value="Upload File">
<input type="button" onclick="withHtmlString()" value="Insert Html String">
<div class="align-center" id="withHtmlString">
	<h2>Html View In PDF</h2>
	<form action="generatePDF" method="POST">
		<label>Enter File Name</label><br>
		<input type="text" name="fileName" required="required"/>
		<br>
		<label>Enter HTML code</label><br>
		<textarea rows="10" cols="50" name="htmlString" required="required"></textarea>
		<br><br>
		<input type="submit" value="Download PDF" class="align-center">
	</form>
</div>
<div class="align-center hidden" id="withFile">
	<h2>Html View In PDF</h2>
	<form action="generatePDFFromFile" method="POST" enctype="multipart/form-data">
		<label>Enter File Name</label><br>
		<input type="text" name="fileName" required="required"/>
		<br>
		<label>Select Fiile</label><br>
		<input type="file" name="htmlFile" required="required" style="border: 1px solid;">
		<br><br>
		<input type="submit" value="Download PDF" class="align-center">
	</form>
</div>
</body>
<script>
function withFile(){
	document.getElementById('withHtmlString').classList.add('hidden');
	document.getElementById('withFile').classList.remove('hidden');
}
function withHtmlString(){
	document.getElementById('withHtmlString').classList.remove('hidden');
	document.getElementById('withFile').classList.add('hidden');
}
</script>
</html>