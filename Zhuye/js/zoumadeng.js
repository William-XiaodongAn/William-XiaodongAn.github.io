    var responsiveSlider = function(A,B,C,D) {

var slider = document.getElementById(A);
var sliderWidth = slider.offsetWidth;
var slideList = document.getElementById(B);
var count = 1;
var items = slideList.querySelectorAll("li").length;
var prev = document.getElementById(C);
var next = document.getElementById(D);

window.addEventListener('resize', function() {
  sliderWidth = slider.offsetWidth;
});

var prevSlide = function() {
  if(count > 1) {
    count = count - 2;
    slideList.style.left = "-" + count * sliderWidth + "px";
    count++;
  }
  else if(count = 1) {
    count = items - 1;
    slideList.style.left = "-" + count * sliderWidth + "px";
    count++;
  }
};

var nextSlide = function() {
  if(count < items) {
    slideList.style.left = "-" + count * sliderWidth + "px";
    count++;
  }
  else if(count = items) {
    slideList.style.left = "0px";
    count = 1;
  }
};

next.addEventListener("click", function() {
  nextSlide();
});

prev.addEventListener("click", function() {
  prevSlide();
});

setInterval(function() {
  nextSlide()
}, 8000);

};

window.onload = function() {
responsiveSlider("slider1","slideWrap1","prev1","next1");  
responsiveSlider("slider2","slideWrap2","prev2","next2");  
responsiveSlider("slider3","slideWrap3","prev3","next3");  
responsiveSlider("slider4","slideWrap4","prev4","next4");  
responsiveSlider("slider5","slideWrap5","prev5","next5");  
}


  