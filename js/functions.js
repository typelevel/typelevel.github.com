
// Scroll
window.onscroll = function() {scrollFunction()};
function scrollFunction() {
  if (document.body.scrollTop > 24 || document.documentElement.scrollTop > 24) {
    document.getElementById("navigation").classList.add('navigation-scroll');
  } else {
    document.getElementById("navigation").classList.remove('navigation-scroll');
  }
}

// Responsive
function responsiveFunction() {
  var x = document.getElementById("navbar-main");
  if (x.className === "navbar") {
    x.className += " responsive";
  } else {
    x.className = "navbar";
  }
}
