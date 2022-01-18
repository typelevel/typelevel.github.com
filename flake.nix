{
  description = "Virtual environment for typelevel.org site";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    devshell.url = "github:numtide/devshell";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, devshell, ... }:
    let
      forSystem = system:
        let
          pkgs = import nixpkgs {
            inherit system;
            overlays = [ devshell.overlay ];
          };

          gems = pkgs.bundlerEnv {
            name = "typelevel-org-bundler-env";
            inherit (pkgs) ruby;
            gemdir = ./.;
          };
        in
        {
          devShell = pkgs.devshell.mkShell {
            name = "typelevel-org-shell";
            commands = [
              {
                name = "jekyll";
                help = "a jekyll bundled with this site's dependencies";
                command = "${gems}/bin/jekyll $@";
              }
              {
                name = "tl-preview";
                help = "preview the Jekyll site";
                command = "${gems}/bin/jekyll serve -wl --baseurl ''";
              }
            ];
          };
        };
    in
    flake-utils.lib.eachDefaultSystem forSystem;
}
