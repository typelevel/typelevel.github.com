$(function() {
  $("body").removeClass("no-js");

  // Subnavigation hide/show
  $('.js-subnav').click(function(e) {
    e.stopPropagation();
    $(this).next(".subnav").toggleClass('visible');
  });

  // Mobile navigation hide/show
  $('.header__menu').click(function(e) {
    e.stopPropagation();
    $(this).next(".navigation").toggleClass('visible');
  });

  // Navigation hide
  $(document).click(function(e) {
    $(".subnav").removeClass('visible');
    $(".navigation").removeClass('visible');
  });

  // Smooth scrolling for #links
  $('a[href*=#]:not([href=#])').click(function() {
    if (location.pathname.replace(/^\//,'') == this.pathname.replace(/^\//,'') && location.hostname == this.hostname) {
      var target = $(this.hash);
      target = target.length ? target : $('[name=' + this.hash.slice(1) +']');
      if (target.length) {
        $('html,body').animate({
          scrollTop: target.offset().top - 20
        }, 1000);
        return false;
      }
    }
  });

  // Expandable projects
  $('.expandable .projects__project-body p').text(function() {
    var text = $(this).text();
    if(text.length > 140) {
      $(this).attr('original', text);
      text = text.substr(0, 115);
      text = text + '...';
      $(this).text(text);
      $(this).append('<span class="expand">Expand</span>');
    }
  });

  $('.expand').click(function() {
    var original = $(this).parent().attr('original');
    $(this).closest('.projects__project-body').addClass('expanded');
    $(this).parent().text(original);
  });

  if ( $(window).width() < 480 ) {
    // Expandable core projects
    $('.expandable-core .projects__project-body').html(function() {
      $('.projects__project-body-extensions', this).hide();
      $(this).append('<span class="expand-core">Expand</span>');
    });

    $('.expand-core').click(function() {
      $(this).prev('.projects__project-body-extensions').show();
      $(this).remove();
    });
  }

  // CoC expanding
  $('.js-expand-coc').click(function(e) {
    e.preventDefault();
    $('#CoC').show();
    $('html,body').animate({
      scrollTop: $('#CoC').offset().top - 20
    }, 1000);
  });

  fadeInScroll();
});

// Fade in on scroll
$(window).scroll( function(){
  fadeInScroll();
});

function fadeInScroll() {
  $('.js-fade-in').each( function(i){
    var bottom_of_object = $(this).offset().top;
    var bottom_of_window = $(window).scrollTop() + $(window).height();

    if( bottom_of_window > bottom_of_object  ){
        $(this).animate({'opacity':'1'},500);
    }
  });

  // Fixed subnavigation on scroll
  var $header = $('.event__header');
  var $hero = $('.event__hero');
  var $body = $('body');
  var bottom = $hero.position().top + $hero.innerHeight();
  var height = $header.outerHeight(true);

  $(window).on('scroll', function(){
    if($(window).scrollTop() >= bottom && !$body.hasClass('fixed')){
      $body.addClass('fixed');
      $hero.css("padding-top", height);
    }
    else if($(window).scrollTop() < bottom && $body.hasClass('fixed')){
      $body.removeClass('fixed');
      $hero.css("padding-top", 0);
    }
});
}
