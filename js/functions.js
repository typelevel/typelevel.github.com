
// Scroll
$(window).on("load", function () {
    $(window).scroll(function () {
        if ($("#navigation").offset().top > 0) {
            $("#navigation").addClass("navigation-scroll");
        } else {
            $("#navigation").removeClass("navigation-scroll");
        }
    });
});

// Responsive
function myFunction() {
  var x = document.getElementById("navbar-main");
  if (x.className === "navbar") {
    x.className += " responsive";
  } else {
    x.className = "navbar";
  }
}
