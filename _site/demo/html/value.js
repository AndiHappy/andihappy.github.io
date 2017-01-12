document.write(" <center> <h1>This is a heading</h1> </center> ");
document.write("<p>This is a paragraph.</p>");

function onclik(){
	alert("welcome，flyBread !");
};

function myFunction()
{
x=document.getElementById("demo");  // 找到元素
x.innerHTML="Hello JavaScript!";    // 改变内容
// 改变样式
x.style.color="#ff0000";   
};

//改变图片的格式
function changeImage(){
	element=document.getElementById('myimage');
	if (element.src.match("bulbon")) {
		element.src="./img/eg_bulboff.gif";
	}else{
		element.src="./img/eg_bulbon.gif";
	}
};


function check()
{
var x=document.getElementById("input").value;
if(x==""||isNaN(x))
	{
	alert("请输入数字，您输入的不是数字。");
	}else{
		document.getElementById("input").innerHTML = "您输入的是数字："+x;
	}
};